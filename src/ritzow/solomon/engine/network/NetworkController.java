package ritzow.solomon.engine.network;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.util.Exitable;
import ritzow.solomon.engine.util.Installable;
import ritzow.solomon.engine.util.MutableInteger;

/** Provides common functionality of the client and server. Manages incoming and outgoing packets. **/
abstract class NetworkController implements Installable, Runnable, Exitable {
	private volatile boolean setupComplete, exit, finished;
	private final DatagramSocket socket;
	private final List<MessageAddressPair> reliableQueue;
	private final Map<SocketAddress, MutableInteger> lastReceived;
	
	protected NetworkController(SocketAddress bindAddress) throws SocketException {
		socket = new DatagramSocket(bindAddress);
		reliableQueue = new LinkedList<MessageAddressPair>();
		lastReceived = new HashMap<SocketAddress, MutableInteger>();
	}
	
	protected void sendUnreliable(SocketAddress recipient, int messageID, byte[] data) {
		if(messageID < 0)
			throw new RuntimeException("messageID must be greater than or equal to zero");
		else if(data.length > Protocol.MAX_MESSAGE_LENGTH)
			throw new RuntimeException("message length is greater than maximum allowed (" + Protocol.MAX_MESSAGE_LENGTH + " bytes)");
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
	 * @param recipient the SocketAddress to send data to
	 * @param messageID the unique ID of the message, must be one greater than the last message sent to the specified recipient
	 * @param data the data to send to the recipient, including any protocol or other data
	 */
	protected void sendReliable(SocketAddress recipient, int messageID, byte[] data, int attempts, int resendInterval) {
		if(messageID < 0)
			throw new RuntimeException("messageID must be greater than or equal to zero");
		else if(data.length > Protocol.MAX_MESSAGE_LENGTH)
			throw new RuntimeException("message length is greater than maximum allowance of " + Protocol.MAX_MESSAGE_LENGTH + " bytes");
		byte[] packet = new byte[5 + data.length];
		ByteUtil.putInteger(packet, 0, messageID);
		ByteUtil.putBoolean(packet, 4, true);
		ByteUtil.copy(data, packet, 5);
		DatagramPacket datagram = new DatagramPacket(packet, packet.length, recipient);
		MessageAddressPair pair = new MessageAddressPair(recipient, messageID);
		
		synchronized(pair) {
			synchronized(reliableQueue) {
				reliableQueue.add(pair);
			}
			
			int attemptsRemaining = attempts;
			
			while((attempts == 0 || attemptsRemaining > 0) && !pair.received) {
				try {
					socket.send(datagram);
					attemptsRemaining--;
					pair.wait(resendInterval);
				} catch (InterruptedException | IOException e) {
					e.printStackTrace();
					continue;
				}
			}
			
			if(!pair.received) {
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
			e.printStackTrace();
		}
	}
	
	protected void removeSender(SocketAddress address) {
		synchronized(lastReceived) {
			lastReceived.remove(address);
		}
	}
	
	/**
	 * implemented by a client/server to process any incoming packets. Packets are not guaranteed to be received in order, and message responses must be handled in this method.
	 * @param messageID the unique ID of the message received
	 * @param protocol the type of message receieved
	 * @param sender the address the message was received from
	 * @param data the body of the message received
	 */
	protected abstract void process(SocketAddress sender, int messageID, byte[] data);
	
	@Override
	public void run() {
		//Create the thread dispatcher for processing received messages
		ExecutorService dispatcher = Executors.newCachedThreadPool();
		
		//Create the buffer DatagramPacket that is the maximum length a message can be plus the 5 header bytes (messageID and reliable flag)
		DatagramPacket buffer = new DatagramPacket(new byte[Protocol.MAX_MESSAGE_LENGTH + 5], Protocol.MAX_MESSAGE_LENGTH + 5);
		
		synchronized(this) {
			setupComplete = true;
			this.notifyAll();
		}

		while(!exit) {
			try {
				//wait for a packet to be received
				socket.receive(buffer);
				
				//ignore received packets that are too short
				if(buffer.getLength() < 5)
					continue;
				
				//parse the packet information
				final SocketAddress sender = 	buffer.getSocketAddress();
				final int messageID = 			ByteUtil.getInteger(buffer.getData(), buffer.getOffset());
				final boolean reliable = 		ByteUtil.getBoolean(buffer.getData(), buffer.getOffset() + 4);
				final byte[] data = 			Arrays.copyOfRange(buffer.getData(), buffer.getOffset() + 5, buffer.getLength());

				//if message is a response, rather than data
				if(messageID == -1) {
					int responseMessageID = ByteUtil.getInteger(data, 0);
					synchronized(reliableQueue) {
						Iterator<MessageAddressPair> iterator = reliableQueue.iterator();
						while(iterator.hasNext()) {
							MessageAddressPair pair = iterator.next();
							if(pair.messageID == responseMessageID && pair.recipient.equals(sender)) {
								synchronized(pair) {
									pair.received = true;
									pair.notifyAll();
								}
								iterator.remove();
							}
						}
					}
				} else if(reliable) { //handle reliable messages by first checking if the received message is reliable
					if(!lastReceived.containsKey(sender)) { //if sender isn't registered yet, add it to hashmap, if the ack isn't received, it will be resent on next send
						lastReceived.put(sender, new MutableInteger(messageID));
						dispatcher.execute(() -> process(sender, messageID, data));
						sendResponse(sender, messageID);
					} else if(messageID == lastReceived.get(sender).intValue() + 1) { //if the message is the next one, process it and update last message
						lastReceived.get(sender).set(messageID);
						dispatcher.execute(() -> process(sender, messageID, data));
						sendResponse(sender, messageID);
					} else { //if the message was already received
						sendResponse(sender, messageID);
						continue; //dont process the message because it isn't new
					}
				} else { //if the message isnt a response and isn't reliable, process it without doing anything else!
					dispatcher.execute(() -> process(sender, messageID, data));
				}
			} catch(SocketException e) {
				if(!socket.isClosed())
					e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			dispatcher.shutdown();
			dispatcher.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			socket.close();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		synchronized(this) {
			finished = true;
			this.notifyAll();
		}
	}
	
	public SocketAddress getSocketAddress() {
		return socket.getLocalSocketAddress();
	}
	
	@Override
	public boolean isSetupComplete() {
		return setupComplete;
	}
	
	@Override
	public void exit() {
		exit = true;
		socket.close();
	}
	
	@Override
	public boolean isFinished() {
		return finished;
	}
}
