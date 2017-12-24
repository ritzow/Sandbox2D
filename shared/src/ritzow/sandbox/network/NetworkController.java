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
import java.util.function.Consumer;
import ritzow.sandbox.data.UncheckedByteArrayDataWriter;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.data.ByteUtil;
import ritzow.sandbox.data.DataWriter;

/** Provides common functionality of the client and server. Manages incoming and outgoing packets. **/
public class NetworkController {
	
	private static final class MessageAddressPair {
		private final InetSocketAddress recipient;
		private final int messageID;
		private volatile boolean received;
		
		public MessageAddressPair(InetSocketAddress recipient, int messageID) {
			this.messageID = messageID;
			this.recipient = recipient;
		}
	}
	
	private static final class MessageIDHolder {
	    private int reliableMessageID, unreliableMessageID;
	    
	    public MessageIDHolder(int reliable, int unreliable) {
	        this.reliableMessageID = reliable;
	        this.unreliableMessageID = unreliable;
	    }
	}
	
	//fields
	private final DatagramSocket socket;
	private final Queue<MessageAddressPair> reliableQueue;
	private final Map<InetSocketAddress, MessageIDHolder> lastReceived;
	private final ThreadLocal<DatagramPacket> packets;
	private final MessageProcessor messageProcessor;
	private volatile boolean started, exit;
	
	/** Message Type **/
	private static final byte RESPONSE_TYPE = 1, RELIABLE_TYPE = 2, UNRELIABLE_TYPE = 3;
	
	public NetworkController(InetSocketAddress bindAddress, MessageProcessor processor) throws SocketException {
		messageProcessor = processor;
		socket = new DatagramSocket(bindAddress);
		reliableQueue = new ConcurrentLinkedQueue<MessageAddressPair>();
		lastReceived = Collections.synchronizedMap(new HashMap<InetSocketAddress, MessageIDHolder>());
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
	public static interface MessageProcessor {
		void process(InetSocketAddress sender, int messageID, byte[] data);
	}
	
	/**
	 * Send a message reliably, blocking until the message is received or a specified number 
	 * of attempts have been made to send the message.
	 * @param recipient the SocketAddress to send data to
	 * @param messageID the unique ID of the message, must be one greater than the last message sent to the specified recipient
	 * @param data the data to send to the recipient, including any protocol or other data
	 * @throws TimeoutException if all send attempts have occurred but no message was received
	 */
	public void sendReliable(InetSocketAddress recipient, int messageID, byte[] data, int attempts, int resendInterval) throws TimeoutException {
		if(attempts < 1)
			throw new IllegalArgumentException("attempts must be greater than 0");
		if(resendInterval < 0)
			throw new IllegalArgumentException("resendInterval must be zero or positive");
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
	
	public void sendUnreliable(InetSocketAddress recipient, int messageID, byte[] data) {
		checkID(messageID);
		try {
			socket.send(getPacket(recipient, RELIABLE_TYPE, messageID, data));
		} catch(IOException e) {
			e.printStackTrace();
			stop();
		}
	}
	
	public void sendUnreliable(InetSocketAddress recipient, int messageID, Consumer<DataWriter> data) {
		checkID(messageID);
		try {
			socket.send(getPacket(recipient, UNRELIABLE_TYPE, messageID, data));
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
	
	private static void checkID(int messageID) {
		if(messageID < -1)
			throw new IllegalArgumentException("messageID must be greater than or equal to zero");
	}
	
	private DatagramPacket getPacket(SocketAddress recipient, byte type, int messageID, byte[] data) {
		DatagramPacket datagram = packets.get();
		setupDatagram(datagram, recipient, type, messageID, data.length);
		ByteUtil.copy(data, datagram.getData(), 5); //put data
		return datagram;
	}
	
	private DatagramPacket getPacket(SocketAddress recipient, byte type, int messageID, Consumer<DataWriter> data) {
		DatagramPacket datagram = packets.get();
		UncheckedByteArrayDataWriter writer = new UncheckedByteArrayDataWriter(datagram.getData(), 5);
		data.accept(writer);
		setupDatagram(datagram, recipient, type, messageID, writer.index()-5);
		return datagram;
	}
	
	private static void setupDatagram(DatagramPacket datagram, SocketAddress recipient, byte type, int messageID, int dataSize) {
		if(dataSize > Protocol.MAX_MESSAGE_LENGTH)
			throw new IllegalArgumentException("message length is greater than maximum allowed (" + Protocol.MAX_MESSAGE_LENGTH + " bytes)");
		datagram.setSocketAddress(recipient);
		datagram.getData()[0] = type;
		ByteUtil.putInteger(datagram.getData(), 1, messageID);
		datagram.setLength(dataSize + 5);
	}
	
	public void removeSender(SocketAddress address) {
		lastReceived.remove(address);
	}
	
	/**
	 * Removes all connections
	 */
	public void removeAllSenders() {
		lastReceived.clear();
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
	
	private void run() {
		//Create the buffer DatagramPacket that is the maximum length a message can be 
		//plus the 5 header bytes (messageID and reliable flag)
		DatagramPacket buffer = new DatagramPacket(new byte[Protocol.MAX_MESSAGE_LENGTH + 5], Protocol.MAX_MESSAGE_LENGTH + 5);

		while(!exit) {
			try {
				socket.receive(buffer); //wait for a packet to be received
			} catch(SocketException e) {
				if(!socket.isClosed()) {
					e.printStackTrace();
					continue;
				}
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
			
			//ignore received packets that are not large enough to contain the full header
			if(buffer.getLength() >= 5) {
				//parse the packet information
				final InetSocketAddress sender = (InetSocketAddress)buffer.getSocketAddress();
				final byte type = buffer.getData()[buffer.getOffset()];

				if(type == RESPONSE_TYPE) {
					int responseMessageID = ByteUtil.getInteger(buffer.getData(), buffer.getOffset() + 1);
					synchronized(reliableQueue) {
						Iterator<MessageAddressPair> iterator = reliableQueue.iterator();
						while(iterator.hasNext()) {
							MessageAddressPair pair = iterator.next();
							if(pair.recipient.equals(sender)) { //find first message addressed to the sender of the ack
								if(pair.messageID == responseMessageID) { //if the ack is for the correct message (oldest awaiting ack)
									reliableQueue.remove(pair);
									pair.received = true;
									Utility.notify(pair); //notify waiting send method
								} break;
							}
						}
					}
				} else {
					synchronized(lastReceived) {
						final int messageID = ByteUtil.getInteger(buffer.getData(), buffer.getOffset() + 1);
						MessageIDHolder holder = lastReceived.get(sender);
						if(type == RELIABLE_TYPE) { //message is reliable
							if(holder == null) {
								//if sender isn't registered yet, add it to hashmap,
								//if the ack isn't received, it will be resent on next send
								lastReceived.put(sender, new MessageIDHolder(messageID, -1));
								messageProcessor.process(sender, messageID, getDataCopy(buffer));
							} else if(messageID == holder.reliableMessageID + 1) {
								//if the message is the next one, process it and update last message
								holder.reliableMessageID = messageID;
								messageProcessor.process(sender, messageID, getDataCopy(buffer));
							} //else: already received, or too early
							sendResponse(sender, messageID);
						} else if(type == UNRELIABLE_TYPE) { //message is unreliable
							if(holder == null) {
								lastReceived.put(sender, new MessageIDHolder(0, messageID));
								messageProcessor.process(sender, messageID, getDataCopy(buffer));
							} else if(messageID > holder.unreliableMessageID) {
								holder.unreliableMessageID = messageID;
								messageProcessor.process(sender, messageID, getDataCopy(buffer));
							}
						}
					}
				}
			}
		}
	}
	
	private static byte[] getDataCopy(DatagramPacket buffer) {
		return Arrays.copyOfRange(buffer.getData(), buffer.getOffset() + 5, buffer.getOffset() + buffer.getLength());
	}
}
