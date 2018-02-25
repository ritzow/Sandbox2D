package ritzow.sandbox.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import ritzow.sandbox.util.Utility;

/** Provides common functionality of the client and server. Manages incoming and outgoing packets. **/
public class NetworkController {
	
	private static final int STARTING_SEND_ID = 0;
	
	private static final class MessageAddressPair {
		private final InetSocketAddress recipient;
		private final int messageID;
		private volatile boolean received;
		
		public MessageAddressPair(InetSocketAddress recipient, int messageID) {
			this.messageID = messageID;
			this.recipient = recipient;
		}
	}
	
	private static final class ConnectionState {
	    private int nextReliableReceiveID, nextUnreliableReceiveID;
	    private final AtomicInteger reliableSendID, unreliableSendID;
	    
	    public ConnectionState() {
	        this.nextReliableReceiveID = 0;
	        this.nextUnreliableReceiveID = 0;
	        this.reliableSendID = new AtomicInteger(STARTING_SEND_ID);
	        this.unreliableSendID = new AtomicInteger(STARTING_SEND_ID);
	    }
	    
	    public int nextReliableSendID() {
	    	return reliableSendID.getAndIncrement();
	    }
	    
	    public int nextUnreliableSendID() {
	    	return unreliableSendID.getAndIncrement();
	    }
	}
	
	private final DatagramChannel channel;
	private final Queue<MessageAddressPair> reliableQueue;
	private final Map<InetSocketAddress, ConnectionState> connections;
	private final ThreadLocal<ByteBuffer> packets;
	private final MessageProcessor messageProcessor;
	private volatile boolean started, exit;
	
	private static final int HEADER_SIZE = 5;
	
	/** Message Type **/
	private static final byte RESPONSE_TYPE = 1, RELIABLE_TYPE = 2, UNRELIABLE_TYPE = 3;
	
	public NetworkController(InetSocketAddress bindAddress, MessageProcessor processor) {
		try {
			messageProcessor = processor;
			channel = DatagramChannel.open().bind(bindAddress);
			reliableQueue = new ConcurrentLinkedQueue<MessageAddressPair>();
			connections = Collections.synchronizedMap(new HashMap<InetSocketAddress, ConnectionState>());
			packets = ThreadLocal.withInitial(() -> ByteBuffer.allocate(HEADER_SIZE + Protocol.MAX_MESSAGE_LENGTH));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * implemented by a client/server to process any incoming packets. 
	 * Packets are not guaranteed to be received in order, 
	 * and message responses must be handled in this method.
	 * @param messageID the unique ID of the message received
	 * @param protocol the type of message received
	 * @param sender the address the message was received from
	 * @param data the body of the message received
	 */	
	@FunctionalInterface
	public interface MessageProcessor {
		void process(InetSocketAddress sender, int messageID, byte[] data);
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
		int messageID = getState(recipient).nextReliableSendID();
		MessageAddressPair pair = new MessageAddressPair(recipient, messageID);
		synchronized(pair) {
			reliableQueue.add(pair);
			int attemptsRemaining = attempts;
			while(attemptsRemaining > 0 && !pair.received && channel.isOpen()) {
				try {
					channel.send(getBuffer(RELIABLE_TYPE, messageID, data), recipient);
					attemptsRemaining--;
					pair.wait(resendInterval); //wait for ack to be received by network controller receiver
				} catch (InterruptedException | IOException e) {
					throw new RuntimeException(e);
				}
			}
			
			if(!pair.received) {
				reliableQueue.remove(pair); //remove the timed out message
				throw new TimeoutException();
			}
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
			e.printStackTrace();
			stop();
		}
	}
	
	private void sendResponse(SocketAddress recipient, int receivedMessageID) {
		try {
			channel.send(ByteBuffer.allocate(5).put(RESPONSE_TYPE).putInt(receivedMessageID).flip(), recipient);
		} catch (IOException e) {
			e.printStackTrace();
			stop();
		}
	}
	
	private ByteBuffer getBuffer(byte type, int messageID, byte[] data) {
		ByteBuffer buffer = packets.get();
		buffer.limit(buffer.capacity());
		return buffer.rewind().put(type).putInt(messageID).put(data).flip();
	}
	
	public void removeConnection(InetSocketAddress address) {
		connections.remove(address);
	}
	
	public void removeAllConnections() {
		connections.clear();
	}
	
	public void start() {
		if(started)
			throw new IllegalStateException("network controller already started");
		new Thread(this::run, "Network Controller").start();
		started = true;
	}
	
	public void stop() {
		exit = true;
		try {
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
	
	private static byte[] getDataCopy(ByteBuffer buffer) {
		byte[] data = new byte[buffer.position() - 5];
		buffer.position(5).get(data);
		return data;
	}
	
	private ConnectionState getState(InetSocketAddress address) {
		ConnectionState state = connections.get(address);
		if(state == null)
			connections.put(address, state = new ConnectionState());
		return state;
	}
	
	private void run() {
		//Create the buffer DatagramPacket that is the maximum length a message can be 
		//plus the 5 header bytes (type and messageID)
		ByteBuffer buffer = ByteBuffer.allocateDirect(HEADER_SIZE + Protocol.MAX_MESSAGE_LENGTH);
		
		while(!exit) {
			try {
				buffer.rewind();
				InetSocketAddress sender = (InetSocketAddress)channel.receive(buffer); //wait for a packet to be received
				//ignore received packets that are not large enough to contain the full header
				if(buffer.position() >= 5) {
					//type of message (RESPONSE, RELIABLE, UNRELIABLE)
					byte type = buffer.get(0);
					//received ID or messageID for ack.
					int messageID = buffer.getInt(1);
					processPacket(buffer, sender, type, messageID);
				}
			} catch(AsynchronousCloseException e) {
				//socket has been closed, do nothing
			} catch (IOException e) {
				e.printStackTrace(); //print exception and continue
			}
		}
	}
	
	private void processPacket(ByteBuffer buffer, InetSocketAddress sender, byte type, int messageID) {
		switch(type) {
		case RESPONSE_TYPE:
			synchronized(reliableQueue) {
				Iterator<MessageAddressPair> iterator = reliableQueue.iterator();
				while(iterator.hasNext()) {
					MessageAddressPair pair = iterator.next();
					if(pair.recipient.equals(sender)) { //find first message addressed to the sender of the ack
						if(pair.messageID == messageID) { //if the ack is for the correct message (oldest awaiting ack)
							iterator.remove();
							pair.received = true;
							Utility.notify(pair); //notify waiting send method
						} break;
					}
				}
			} break;
		case RELIABLE_TYPE:
			ConnectionState reliableState = getState(sender);
			if(messageID == reliableState.nextReliableReceiveID) {
				//if the message is the next one, process it and update last message
				InetSocketAddress address = sender;
				sendResponse(address, messageID);
				reliableState.nextReliableReceiveID++;
				messageProcessor.process(address, messageID, getDataCopy(buffer));
			} else if(messageID < reliableState.nextReliableReceiveID) { //message already received
				sendResponse(sender, messageID);
			} break; //else: message received too early
		case UNRELIABLE_TYPE:
			ConnectionState unreliableState = getState(sender);
			if(messageID >= unreliableState.nextUnreliableReceiveID) {
				unreliableState.nextUnreliableReceiveID = messageID + 1;
				messageProcessor.process(sender, messageID, getDataCopy(buffer));
			} break; //else: message is outdated
		}
	}
}
