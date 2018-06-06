package ritzow.sandbox.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import ritzow.sandbox.util.Utility;

/** Provides common functionality of the client and server. Manages incoming and outgoing packets. **/
public class NetworkController {
	private final DatagramChannel channel;
	private final Map<InetSocketAddress, ConnectionState> connections;
	private final ThreadLocal<ByteBuffer> packets;
	private final MessageProcessor messageProcessor;
	private volatile boolean started, exit;
	
	private static final int STARTING_SEND_ID = 0;
	private static final int HEADER_SIZE = 5, MAX_PACKET_SIZE = HEADER_SIZE + Protocol.MAX_MESSAGE_LENGTH;
	private static final byte RESPONSE_TYPE = 1, RELIABLE_TYPE = 2, UNRELIABLE_TYPE = 3;
	
	public NetworkController(InetSocketAddress bindAddress, MessageProcessor processor) throws IOException {
		messageProcessor = processor;
		channel = DatagramChannel.open().bind(bindAddress)
				.setOption(StandardSocketOptions.SO_SNDBUF, MAX_PACKET_SIZE);
		connections = new ConcurrentHashMap<>();
		packets = ThreadLocal.withInitial(() -> ByteBuffer.allocate(MAX_PACKET_SIZE));
	}
	
	public void start() {
		if(started)
			throw new IllegalStateException("network controller already started");
		new Thread(this::runAsyncReceiving, "Network Controller").start();
		started = true;
	}
	
	public void stop() {
		if(!started)
			throw new IllegalStateException("network controller has not been started");
		try {
			exit = true;
			channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public InetSocketAddress getBindAddress() {
		try {
			return (InetSocketAddress)channel.getLocalAddress();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void removeConnection(InetSocketAddress address) {
		connections.remove(address);
	}
	
	public void removeAllConnections() {
		connections.clear();
	}
	
	/**
	 * implemented by a client/server to process any incoming packets. 
	 * Packets are not guaranteed to be received in order, 
	 * and message responses must be handled in this method.
	 * @param sender the address the message was received from
	 * @param data the body of the message received
	 */	
	@FunctionalInterface
	public interface MessageProcessor {
		void process(InetSocketAddress sender, byte[] data);
	}
	
	/**
	 * Send a message reliably, blocking until the message is received or a specified number 
	 * of attempts have been made to send the message.
	 * @param recipient the address to send the data to.
	 * @param messageID the unique ID of the message, must be one greater than the last message sent to the specified recipient.
	 * @param data the data to send to the recipient.
	 * @throws TimeoutException if all send attempts have occurred but no message was received
	 */
	public void sendReliable(InetSocketAddress recipient, byte[] data, int attempts, int resendInterval) throws TimeoutException {
		if(attempts < 1)
			throw new IllegalArgumentException("attempts must be greater than 0");
		if(resendInterval < 0)
			throw new IllegalArgumentException("resendInterval must be greater than or equal to zero");
		ConnectionState state = getState(recipient);
		synchronized(state) { //prevent other sendReliable calls from immediately queuing the next message
			var pair = state.queue();
			ByteBuffer buffer = getBuffer(RELIABLE_TYPE, pair.messageID, data);
			int attemptsRemaining = attempts;
			synchronized(pair) {
				while(attemptsRemaining > 0 && !pair.received && channel.isOpen()) {
					try {
						if(attemptsRemaining < attempts)
							System.err.println(attemptsRemaining + " attempts remaining");
						channel.send(buffer, recipient);
						attemptsRemaining--;
						pair.wait(resendInterval); //wait for ack to be received by network controller receiver	
					} catch(InterruptedException | IOException e) {
						throw new RuntimeException(e);
					}	
				}	
			}
			if(!pair.received)
				throw new TimeoutException();
		}
	}

	/**
	 * Sends a message without ensuring it is received by the recipient.
	 * @param recipient the address that should receive the message.
	 * @param data the message data.
	 */
	public void sendUnreliable(InetSocketAddress recipient, byte[] data) {
		try {
			channel.send(getBuffer(UNRELIABLE_TYPE, getState(recipient).nextUnreliableSendID(), data), recipient);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void sendResponse(InetSocketAddress recipient, ByteBuffer sendBuffer, int receivedMessageID) {
		try {
			channel.send(sendBuffer.position(1).putInt(receivedMessageID).flip(), recipient);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private ByteBuffer getBuffer(byte type, int messageID, byte[] data) {
		ByteBuffer buffer = packets.get();
		buffer.limit(buffer.capacity());
		return buffer.rewind().put(type).putInt(messageID).put(data).flip();
	}
	
	private static byte[] getDataCopy(ByteBuffer buffer) {
		byte[] data = new byte[buffer.position() - HEADER_SIZE];
		buffer.position(HEADER_SIZE).get(data);
		return data;
	}
	
	private ConnectionState getState(InetSocketAddress address) {
		ConnectionState state = connections.get(address);
		if(state == null)
			connections.put(address, state = new ConnectionState());
		return state;
	}
	
	private void runAsyncReceiving() {
		ByteBuffer buffer = ByteBuffer.allocateDirect(MAX_PACKET_SIZE);
		ByteBuffer sendBuffer = ByteBuffer.allocateDirect(HEADER_SIZE).put(RESPONSE_TYPE);
		
		while(!exit) {
			try {
				processPacket(receive(buffer), buffer, sendBuffer);
				buffer.rewind();
			} catch(AsynchronousCloseException e) {
				//socket has been closed, do nothing
			} catch (IOException e) {
				e.printStackTrace(); //print exception and continue
			}
		}
	}
	
	private InetSocketAddress receive(ByteBuffer buffer) throws IOException {
		return (InetSocketAddress)channel.receive(buffer); //wait for a packet to be received
	}
	
	private void processPacket(InetSocketAddress sender, ByteBuffer buffer, ByteBuffer sendBuffer) {
		if(sender == null)
			throw new IllegalArgumentException("sender is null");
		//ignore received packets that are not large enough to contain the full header
		if(buffer.position() >= HEADER_SIZE) {
			byte type = buffer.get(0); //type of message (RESPONSE, RELIABLE, UNRELIABLE)
			int messageID = buffer.getInt(1); //received ID or messageID for ack.
			ConnectionState state = getState(sender); //messageIDs and send queue
			switch(type) {
			case RESPONSE_TYPE:
				state.process(messageID);
				break; //else drop the response
			case RELIABLE_TYPE:
				if(messageID == state.nextReliableReceiveID) {
					//if the message is the next one, process it and update last message
					sendResponse(sender, sendBuffer, messageID);
					state.nextReliableReceiveID++;
					messageProcessor.process(sender, getDataCopy(buffer));
				} else if(messageID < state.nextReliableReceiveID) { //message already received
					sendResponse(sender, sendBuffer, messageID);
				} break; //else: message received too early
			case UNRELIABLE_TYPE:
				if(messageID >= state.nextUnreliableReceiveID) {
					state.nextUnreliableReceiveID = messageID + 1;
					messageProcessor.process(sender, getDataCopy(buffer));
				} break; //else: message is outdated
			}
		}
	}
	
	private static final class PacketSendEntry {
		public final int messageID;
		public volatile boolean received;
		
		public PacketSendEntry(int messageID) {
			this.messageID = messageID;
		}
	}
	
	private static final class ConnectionState {
	    public int nextReliableReceiveID, nextUnreliableReceiveID;
	    private final AtomicInteger unreliableSendID;
	    private volatile PacketSendEntry current;
	    
	    public ConnectionState() {
	        this.nextReliableReceiveID = STARTING_SEND_ID;
	        this.nextUnreliableReceiveID = STARTING_SEND_ID;
	        this.unreliableSendID = new AtomicInteger(STARTING_SEND_ID);
	        this.current = new PacketSendEntry(STARTING_SEND_ID - 1);
	    }
	    
	    public PacketSendEntry queue() {
	    	return current = new PacketSendEntry(current.messageID + 1);
	    }
	    
	    public void process(int messageID) {
	    	var pair = current;
			if(!pair.received && pair.messageID == messageID) {
				pair.received = true;
				Utility.notify(pair);
			} 
	    }
	    
	    public int nextUnreliableSendID() {
	    	return unreliableSendID.getAndIncrement();
	    }
	}
}
