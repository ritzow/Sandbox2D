package ritzow.sandbox.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import ritzow.sandbox.data.ByteUtil;
import ritzow.sandbox.data.DataWriter;
import ritzow.sandbox.data.UncheckedByteArrayDataWriter;
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
	
	//fields
	private final DatagramSocket socket;
	private final Queue<MessageAddressPair> reliableQueue;
	private final Map<InetSocketAddress, ConnectionState> connections;
	private final ThreadLocal<DatagramPacket> packets;
	private final MessageProcessor messageProcessor;
	private volatile boolean started, exit;
	
	private static final int HEADER_SIZE = 5;
	
	/** Message Type **/
	private static final byte RESPONSE_TYPE = 1, RELIABLE_TYPE = 2, UNRELIABLE_TYPE = 3;
	
	public NetworkController(InetSocketAddress bindAddress, MessageProcessor processor) throws SocketException {
		messageProcessor = processor;
		socket = new DatagramSocket(bindAddress);
		reliableQueue = new ConcurrentLinkedQueue<MessageAddressPair>();
		connections = Collections.synchronizedMap(new HashMap<InetSocketAddress, ConnectionState>());
		packets = ThreadLocal.withInitial(() -> new DatagramPacket(new byte[Protocol.MAX_MESSAGE_LENGTH + 5], 0));
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
		DatagramPacket packet = getPacket(recipient, RELIABLE_TYPE, messageID, data);
		MessageAddressPair pair = new MessageAddressPair(recipient, messageID);
		synchronized(pair) {
			reliableQueue.add(pair);
			int attemptsRemaining = attempts;
			while(attemptsRemaining > 0 && !pair.received && !socket.isClosed()) {
				try {
					socket.send(packet);
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
			socket.send(getPacket(recipient, UNRELIABLE_TYPE, getState(recipient).nextUnreliableSendID(), data));
		} catch(IOException e) {
			e.printStackTrace();
			stop();
		}
	}
	
	public void sendUnreliable(InetSocketAddress recipient, Consumer<DataWriter> data) {
		try {
			socket.send(getPacket(recipient, UNRELIABLE_TYPE, getState(recipient).nextUnreliableSendID(), data));
		} catch(IOException e) {
			e.printStackTrace();
			stop();
		}
	}
	
	private void sendResponse(SocketAddress recipient, int receivedMessageID) {
		try {
			DatagramPacket datagram = packets.get();
			setupDatagram(datagram, recipient, RESPONSE_TYPE, receivedMessageID, 0);
			socket.send(datagram);
		} catch (IOException e) {
			e.printStackTrace();
			stop();
		}
	}
	
	private DatagramPacket getPacket(SocketAddress recipient, byte type, int messageID, byte[] data) {
		DatagramPacket datagram = packets.get();
		setupDatagram(datagram, recipient, type, messageID, data.length);
		ByteUtil.copy(data, datagram.getData(), datagram.getOffset() + 5); //put data
		return datagram;
	}
	
	private DatagramPacket getPacket(SocketAddress recipient, byte type, int messageID, Consumer<DataWriter> data) {
		DatagramPacket datagram = packets.get();
		UncheckedByteArrayDataWriter writer = new UncheckedByteArrayDataWriter(datagram.getData(), HEADER_SIZE);
		data.accept(writer);
		setupDatagram(datagram, recipient, type, messageID, writer.index() - HEADER_SIZE);
		return datagram;
	}
	
	private static void setupDatagram(DatagramPacket datagram, SocketAddress recipient, byte type, int messageID, int dataSize) {
		if(messageID < 0)
			throw new IllegalArgumentException("messageID cannot be negative");
		if(dataSize > Protocol.MAX_MESSAGE_LENGTH)
			throw new IllegalArgumentException("message length is greater than maximum allowed (" + Protocol.MAX_MESSAGE_LENGTH + " bytes)");
		datagram.setSocketAddress(recipient);
		datagram.getData()[datagram.getOffset()] = type;
		ByteUtil.putInteger(datagram.getData(), datagram.getOffset() + 1, messageID);
		datagram.setLength(dataSize + 5);
	}
	
	public void removeConnection(SocketAddress address) {
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
		socket.close();
	}
	
	public InetSocketAddress getBindAddress() {
		return (InetSocketAddress)socket.getLocalSocketAddress();
	}
	
	private static byte[] getDataCopy(DatagramPacket buffer) {
		return Arrays.copyOfRange(buffer.getData(), buffer.getOffset() + HEADER_SIZE, buffer.getOffset() + buffer.getLength());
	}
	
	private InetSocketAddress getAddress(DatagramPacket packet) { //TODO is this more or less efficient?
		InetSocketAddress address = (InetSocketAddress)packet.getSocketAddress();
		connections.put(address, connections.get(address));
		return address;
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
		int bufferSize = Protocol.MAX_MESSAGE_LENGTH + HEADER_SIZE;
		DatagramPacket buffer = new DatagramPacket(new byte[bufferSize], bufferSize);

		while(!exit) {
			try {
				socket.receive(buffer); //wait for a packet to be received
				//ignore received packets that are not large enough to contain the full header
				if(buffer.getLength() >= 5) {
					InetSocketAddress sender = getAddress(buffer);
					//type of message (RESPONSE, RELIABLE, UNRELIABLE)
					byte type = buffer.getData()[buffer.getOffset()];
					//received ID or messageID for ack.
					int messageID = ByteUtil.getInteger(buffer.getData(), buffer.getOffset() + 1);
					processPacket(buffer, sender, type, messageID);
				}
			} catch(SocketException e) {
				if(!socket.isClosed())
					e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace(); //print exception and continue
			}
		}
	}
	
	private void processPacket(DatagramPacket buffer, InetSocketAddress sender, byte type, int messageID) {
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
