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
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import ritzow.sandbox.data.ByteUtil;

/** Provides common functionality of the client and server. Manages incoming and outgoing packets. **/
public final class NetworkController {
	private final DatagramSocket socket;
	private final Queue<MessageAddressPair> reliableQueue;
	private final Map<InetSocketAddress, MessageIDHolder> lastReceived;
	private final MessageProcessor messageProcessor;
	private Thread receivingThread;
	private volatile boolean exit;
	
	public NetworkController(InetSocketAddress bindAddress, MessageProcessor processor) throws SocketException {
		socket = new DatagramSocket(bindAddress);
		reliableQueue = new ConcurrentLinkedQueue<MessageAddressPair>();
		lastReceived = Collections.synchronizedMap(new HashMap<>());
		messageProcessor = processor;
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
			byte[] packet = new byte[5 + data.length];
			ByteUtil.putInteger(packet, 0, messageID);
			ByteUtil.putBoolean(packet, 4, false);
			ByteUtil.copy(data, packet, 5);
			socket.send(new DatagramPacket(packet, packet.length, recipient));
		} catch(IOException e) {
			e.printStackTrace();
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
		byte[] packet = new byte[5 + data.length];
		ByteUtil.putInteger(packet, 0, messageID); //put message id
		ByteUtil.putBoolean(packet, 4, true); //put reliable
		ByteUtil.copy(data, packet, 5); //put data
		DatagramPacket datagram = new DatagramPacket(packet, packet.length, recipient);
		MessageAddressPair pair = new MessageAddressPair(recipient, messageID);
		synchronized(pair) {
			reliableQueue.add(pair);
			int attemptsRemaining = attempts;
			while(attemptsRemaining > 0 && !pair.received && !socket.isClosed()) {
				try {
					socket.send(datagram);
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
			byte[] packet = new byte[9];
			ByteUtil.putInteger(packet, 0, -1);
			ByteUtil.putBoolean(packet, 4, false);
			ByteUtil.putInteger(packet, 5, receivedMessageID);
			socket.send(new DatagramPacket(packet, packet.length, recipient));
		} catch (IOException e) {
			stop();
		}
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
		if(receivingThread != null)
			throw new IllegalStateException("network controller already started");
		receivingThread = new Thread(this::run, "Network Controller");
		receivingThread.start();
	}
	
	public void stop() {
		exit = true;
		socket.close();
	}
	
	public InetSocketAddress getSocketAddress() {
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

			if(messageID == -1) { //if message is a response, rather than data
				int responseMessageID = ByteUtil.getInteger(data, 0);
				synchronized(reliableQueue) {
					MessageAddressPair pair = null;
					for(MessageAddressPair p : reliableQueue) { //find first message addressed to the sender of the ack
						if(p.recipient.equals(sender)) {
							pair = p;
							break;
						}
					}
					
					if(pair != null && pair.messageID == responseMessageID) {
						//if the response is for the next message in the queue awaiting confirmation of reception, 
						//it can be removed
						reliableQueue.remove(pair);
						synchronized(pair) {
							pair.received = true;
							pair.notifyAll();
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
