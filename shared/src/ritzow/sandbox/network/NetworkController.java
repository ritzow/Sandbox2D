package ritzow.sandbox.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import ritzow.sandbox.util.Utility;

import static ritzow.sandbox.network.Protocol.*;

/** Provides common functionality of the client and server. Manages incoming and outgoing packets. **/
public class NetworkController {
	private volatile DatagramChannel channel;
	private final Map<InetSocketAddress, ConnectionState> connections;
	private final ThreadLocal<ByteBuffer> packets;
	private final MessageProcessor messageProcessor;
	private volatile boolean started, exit;
	
	public NetworkController(ProtocolFamily family, InetSocketAddress bindAddress, MessageProcessor processor) throws IOException {
		messageProcessor = processor;
		channel = DatagramChannel.open(family).bind(bindAddress)
				.setOption(StandardSocketOptions.SO_SNDBUF, MAX_PACKET_SIZE);
		connections = new ConcurrentHashMap<>();
		packets = ThreadLocal.withInitial(() -> ByteBuffer.allocate(MAX_PACKET_SIZE));
	}
	
	private static DatagramChannel createSocket(SocketAddress bind) throws IOException {
		return DatagramChannel.open().bind(bind)
				.setOption(StandardSocketOptions.SO_SNDBUF, MAX_PACKET_SIZE);
	}
	
	public void start() {
		if(started)
			throw new IllegalStateException("network controller already started");
		new Thread(this::runAsyncReceiving, "Network Controller Receiving").start();
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
	 * @param attempts the number of packet sends before throwing a TimeoutException
	 * @param resendInterval the time between attempts
	 * @throws TimeoutException if all send attempts have occurred but no message was received, 
	 * or the previous call to sendReliable timed out.
	 */
	public void sendReliable(InetSocketAddress recipient, byte[] data, int attempts, int resendInterval) throws TimeoutException, IOException {
		if(attempts < 1)
			throw new IllegalArgumentException("attempts must be greater than 0");
		if(resendInterval < 0)
			throw new IllegalArgumentException("resendInterval must be greater than or equal to zero");
		if(exit || !channel.isOpen())
			throw new IllegalStateException("NetworkController is closed");
		ConnectionState state = getState(recipient);
		synchronized(state) { //prevents other sendReliable calls from immediately queuing the next message
			if(!state.previousReceived())
				throw new TimeoutException("last message not received");
			var pair = state.queue();
			ByteBuffer packet = packets.get().put(RELIABLE_TYPE).putInt(pair.messageID).put(data).flip();
			synchronized(pair) {
				int attemptsRemaining = attempts;
				while(attemptsRemaining > 0 && !pair.received && channel.isOpen()) {
					channel.send(packet, recipient);
					packet.rewind();
					attemptsRemaining--;
					try {
						pair.wait(resendInterval); //wait for ack to be received by network controller receiver
					} catch (InterruptedException e) {
						if(exit || !(connections.containsKey(recipient) && channel.isOpen())) {
							return;
						} else {
							e.printStackTrace();
						}
					}
				}
			
			}
			packet.clear();
			if(!pair.received && channel.isOpen())
				throw new TimeoutException();
		}
	}

	/**
	 * Sends a message without ensuring it is received by the recipient.
	 * @param recipient the address that should receive the message.
	 * @param data the message data.
	 * @throws IOException if a socket error occurs
	 */
	public void sendUnreliable(InetSocketAddress recipient, byte[] data) throws IOException {
		ByteBuffer packet = packets.get();
		channel.send(packet.put(UNRELIABLE_TYPE).putInt(getState(recipient).nextUnreliableSendID()).put(data).flip(), recipient);
		packet.clear();
	}
	
	private void sendResponse(InetSocketAddress recipient, ByteBuffer sendBuffer, int receivedMessageID) throws IOException {
		channel.send(sendBuffer.put(RESPONSE_TYPE).putInt(receivedMessageID).flip(), recipient);
		sendBuffer.clear();
	}
	
	private static byte[] copyPayload(ByteBuffer buffer) {
		byte[] data = new byte[buffer.remaining()];
		buffer.get(data);
		return data;
	}
	
	private ConnectionState getState(InetSocketAddress address) {
		ConnectionState state = connections.get(address);
		if(state == null)
			connections.put(address, state = new ConnectionState());
		return state;
	}
	
	public void setManualReceivingMode() throws IOException {
		if(!channel.isBlocking())
			throw new IllegalStateException("already in manual receiving mode");
		var address = channel.getLocalAddress();
		channel.close();
		channel = createSocket(address);
		channel.configureBlocking(false);
	}
	
	private final ByteBuffer receiveBuffer = ByteBuffer.allocateDirect(MAX_PACKET_SIZE);
	private final ByteBuffer responseSendBuffer = ByteBuffer.allocateDirect(HEADER_SIZE);
	
	public void receive() {
		if(channel.isBlocking())
			throw new IllegalStateException("NetworkController is in blocking mode");
		try {
			InetSocketAddress address;
			while((address = (InetSocketAddress)channel.receive(receiveBuffer)) != null) {
				processPacket(address, receiveBuffer, responseSendBuffer, messageProcessor);				
			}
		} catch (IOException e) {
			e.printStackTrace(); //print exception and continue
			stop();
		}
	}
	
	private void runAsyncReceiving() {
		try {
			while(!exit) {
				processPacket((InetSocketAddress)channel.receive(receiveBuffer), receiveBuffer, responseSendBuffer, messageProcessor);
			}
		} catch(AsynchronousCloseException e) {
			//socket has been closed, do nothing
		} catch (IOException e) {
			e.printStackTrace(); //print exception and continue
			stop();
		}
	}
	
	private void processPacket(InetSocketAddress sender, ByteBuffer buffer, ByteBuffer sendBuffer, MessageProcessor messageProcessor) throws IOException {
		if(sender == null)
			throw new IllegalArgumentException("sender is null");
		buffer.flip(); //flip to set limit and prepare to read packet data
		if(buffer.limit() >= HEADER_SIZE) {
			byte type = buffer.get(); //type of message (RESPONSE, RELIABLE, UNRELIABLE)
			int messageID = buffer.getInt(); //received ID or messageID for ack.
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
					messageProcessor.process(sender, copyPayload(buffer));
				} else if(messageID < state.nextReliableReceiveID) { //message already received
					sendResponse(sender, sendBuffer, messageID);
				} break; //else: message received too early
			case UNRELIABLE_TYPE:
				if(messageID >= state.nextUnreliableReceiveID) {
					state.nextUnreliableReceiveID = messageID + 1;
					messageProcessor.process(sender, copyPayload(buffer));
				} break; //else: message is outdated
			}
		}
		buffer.clear(); //clear to prepare for next receive
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
	        this.current.received = true;
	    }
	    
	    public PacketSendEntry queue() {
	    	return current = new PacketSendEntry(current.messageID + 1);
	    }
	    
	    public boolean previousReceived() {
	    	return current.received;
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
