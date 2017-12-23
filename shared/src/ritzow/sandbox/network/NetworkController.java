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
import ritzow.sandbox.data.ByteUtil;

/** Provides common functionality of the client and server. Manages incoming and outgoing packets. **/
public class NetworkController {
	private final DatagramSocket socket;
	private final Queue<MessageAddressPair> reliableQueue;
	private final Map<InetSocketAddress, MessageIDHolder> lastReceived;
	private final ThreadLocal<DatagramPacket> packets;
	private final MessageProcessor messageProcessor;
	private volatile boolean started, exit;
	
	private static final int RESPONSE_ID = -1;
	
	public NetworkController(InetSocketAddress bindAddress, MessageProcessor processor) throws SocketException {
		messageProcessor = processor;
		socket = new DatagramSocket(bindAddress);
		reliableQueue = new ConcurrentLinkedQueue<MessageAddressPair>();
		lastReceived = Collections.synchronizedMap(new HashMap<InetSocketAddress, MessageIDHolder>());
		packets = new ThreadLocal<DatagramPacket>() {
			protected DatagramPacket initialValue() {
				return new DatagramPacket(new byte[Protocol.MAX_MESSAGE_LENGTH + 5], 0);
			}
		};
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
	
	private static void checkMessage(int messageID, int length) {
		if(messageID < 0)
			throw new IllegalArgumentException("messageID must be greater than or equal to zero");
		else if(length > Protocol.MAX_MESSAGE_LENGTH)
			throw new IllegalArgumentException("message length is greater than maximum allowed (" + 
					Protocol.MAX_MESSAGE_LENGTH + " bytes)");
	}
	
	public void sendUnreliable(InetSocketAddress recipient, int messageID, byte[] data) {
		checkMessage(messageID, data.length);
		try {
			socket.send(getPacket(recipient, false, messageID, data));
		} catch(IOException e) {
			e.printStackTrace();
			stop();
		}
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
		checkMessage(messageID, data.length);
		
		DatagramPacket packet = getPacket(recipient, true, messageID, data);
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
	
	private void sendResponse(SocketAddress recipient, int receivedMessageID) {
		try {
			byte[] data = new byte[4];
			ByteUtil.putInteger(data, 0, receivedMessageID);
			socket.send(getPacket(recipient, false, RESPONSE_ID, data));
		} catch (IOException e) {
			e.printStackTrace();
			stop();
		}
	}
	
	private DatagramPacket getPacket(SocketAddress recipient, boolean reliable, int messageID, byte[] data) {
		DatagramPacket datagram = packets.get();
		datagram.setSocketAddress(recipient);
		ByteUtil.putInteger(datagram.getData(), 0, messageID); //put message id
		ByteUtil.putBoolean(datagram.getData(), 4, reliable);
		ByteUtil.copy(data, datagram.getData(), 5); //put data
		datagram.setLength(data.length + 5);
		return datagram;
	}
	
	public void removeSender(SocketAddress address) {
		lastReceived.remove(address);
	}
	
	/**
	 * Removes all connections
	 */
	public void removeSenders() {
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
	    public int latestMessageID;
	    
	    public MessageIDHolder(int value) {
	        this.latestMessageID = value;
	    }
	}
	
	private void run() {
		//Create the buffer DatagramPacket that is the maximum length a message can be 
		//plus the 5 header bytes (messageID and reliable flag)
		DatagramPacket buffer = new DatagramPacket(new byte[Protocol.MAX_MESSAGE_LENGTH + 5], Protocol.MAX_MESSAGE_LENGTH + 5);

		while(!exit) {
			//wait for a packet to be received
			try {
				socket.receive(buffer);
			} catch(SocketException e) {
				if(!socket.isClosed()) {
					e.printStackTrace();
					continue;
				}
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
			
			//ignore received packets that are not large enough to cont ain the full header
			if(buffer.getLength() < 5) {
				continue;
			}
			
			//parse the packet information
			InetSocketAddress sender = (InetSocketAddress)buffer.getSocketAddress();
			final int messageID = ByteUtil.getInteger(buffer.getData(), buffer.getOffset());
			byte[] data = Arrays.copyOfRange(buffer.getData(), buffer.getOffset() + 5, buffer.getOffset() + buffer.getLength());

			if(messageID == RESPONSE_ID) { //if message is a response, rather than data
				int responseMessageID = ByteUtil.getInteger(data, 0);
				synchronized(reliableQueue) {
					Iterator<MessageAddressPair> iterator = reliableQueue.iterator();
					while(iterator.hasNext()) {
						MessageAddressPair pair = iterator.next();
						if(pair.recipient.equals(sender)) { //find first message addressed to the sender of the ack
							if(pair.messageID == responseMessageID) { //if the ack is for the correct message (oldest awaiting ack)
								reliableQueue.remove(pair);
								synchronized(pair) { //notify waiting send method
									pair.received = true;
									pair.notifyAll();
								}
							} break;
						}
					}
				}
			} else if(ByteUtil.getBoolean(buffer.getData(), buffer.getOffset() + 4)) { //message is reliable
				synchronized(lastReceived) {
					MessageIDHolder holder = lastReceived.get(sender);
					if(holder == null) {
						//if sender isn't registered yet, add it to hashmap,
						//if the ack isn't received, it will be resent on next send
						lastReceived.put(sender, new MessageIDHolder(messageID));
						messageProcessor.process(sender, messageID, data);
					} else if(messageID == holder.latestMessageID + 1) {
						//if the message is the next one, process it and update last message
						holder.latestMessageID = messageID;
						messageProcessor.process(sender, messageID, data);
					}
					sendResponse(sender, messageID);
				}
			} else {
				//if the message isnt a response and isn't reliable, process it without doing anything else!
				messageProcessor.process(sender, messageID, data);
			}
		}
	}
}
