package ritzow.solomon.engine.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.util.Service;

/** Provides common functionality of the client and server. Manages incoming and outgoing packets. **/
abstract class NetworkController implements Service {
	private volatile boolean setupComplete, exit, finished;
	private final DatagramSocket socket;
	private final List<MessageAddressPair> reliableQueue;
	private final Map<SocketAddress, MutableInteger> lastReceived;
	
	protected NetworkController(SocketAddress bindAddress) throws SocketException {
		socket = new DatagramSocket(bindAddress);
		reliableQueue = new LinkedList<MessageAddressPair>();
		lastReceived = new HashMap<SocketAddress, MutableInteger>();
	}
	
	protected final void sendUnreliable(SocketAddress recipient, int messageID, byte[] data) {
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
	 * @throws TimeoutException if all send attempts have occurred but no message was received
	 */
	protected final void sendReliable(SocketAddress recipient, int messageID, byte[] data, int attempts, int resendInterval) throws TimeoutException {
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
			
			while((attempts == 0 || attemptsRemaining > 0) && !pair.received && !socket.isClosed()) {
				try {
					socket.send(datagram);
					attemptsRemaining--;
					pair.wait(resendInterval); //release the lock this method's thread has on pair, and wait for it to be modified/notified by incoming packet thread
				} catch (InterruptedException | IOException e) {
					e.printStackTrace();
					continue;
				}
			}
			
			if(!pair.received) {
				synchronized(reliableQueue) {
					reliableQueue.remove(pair); //remove the timed out message
				}
				
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
	
	protected final void removeSender(SocketAddress address) {
		synchronized(lastReceived) {
			lastReceived.remove(address);
		}
	}
	
	protected final void removeSenders() {
		synchronized(lastReceived) {
			lastReceived.clear();
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
	protected abstract void process(SocketAddress sender, int messageID, byte[] data);
	
	@Override
	public final void run() {
		//Create the thread dispatcher for processing received messages
		ExecutorService dispatcher = Executors.newCachedThreadPool();
		
		//Create the buffer DatagramPacket that is the maximum length a message can be plus the 5 header bytes (messageID and reliable flag)
		DatagramPacket buffer = new DatagramPacket(new byte[Protocol.MAX_MESSAGE_LENGTH + 5], Protocol.MAX_MESSAGE_LENGTH + 5);
		
		synchronized(this) {
			setupComplete = true;
			this.notifyAll();
		}

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

			
			//ignore received packets that are not large enough to contain the full header
			if(buffer.getLength() < 5) {
				continue;
			}
			
			//parse the packet information
			final SocketAddress sender = 	buffer.getSocketAddress();
			final int messageID = 			ByteUtil.getInteger(buffer.getData(), buffer.getOffset());
			final byte[] data = 			Arrays.copyOfRange(buffer.getData(), buffer.getOffset() + 5, buffer.getOffset() + buffer.getLength());

			//if message is a response, rather than data
			if(messageID == -1) {
				int responseMessageID = ByteUtil.getInteger(data, 0);
				synchronized(reliableQueue) {
					Iterator<MessageAddressPair> iterator = reliableQueue.iterator();
					while(iterator.hasNext()) {
						MessageAddressPair pair = iterator.next();
						if(pair.messageID == responseMessageID && pair.recipient.equals(sender)) {
							iterator.remove();
							synchronized(pair) {
								pair.received = true;
								pair.notifyAll();
							}
						}
					}
				}
			} else if(ByteUtil.getBoolean(buffer.getData(), buffer.getOffset() + 4)) {  //message is reliable
				//handle reliable messages by first checking if the received message is reliable
				synchronized(lastReceived) {
					if(!lastReceived.containsKey(sender)) {
						//if sender isn't registered yet, add it to hashmap, if the ack isn't received, it will be resent on next send
						lastReceived.put(sender, new MutableInteger(messageID));
						dispatcher.execute(new PacketRunnable(sender, messageID, data));
						sendResponse(sender, messageID);
					} else if(messageID == lastReceived.get(sender).intValue() + 1) {
						//if the message is the next one, process it and update last message
						lastReceived.get(sender).set(messageID);
						dispatcher.execute(new PacketRunnable(sender, messageID, data));
						sendResponse(sender, messageID);
					} else { 
						//if the message was already received
						sendResponse(sender, messageID);
					}
				}
			} else {
				//if the message isnt a response and isn't reliable, process it without doing anything else!
				dispatcher.execute(new PacketRunnable(sender, messageID, data));
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
	
	private final class PacketRunnable implements Runnable {
		private final SocketAddress address;
		private final int messageID;
		private final byte[] data;
		
		public PacketRunnable(SocketAddress address, int messageID, byte[] data) {
			this.address = address;
			this.messageID = messageID;
			this.data = data;
		}

		@Override
		public void run() {
			process(address, messageID, data);
		}
	}
	
	private static final class MessageAddressPair {
		protected final SocketAddress recipient;
		protected final int messageID;
		protected volatile boolean received;
		
		public MessageAddressPair(SocketAddress recipient, int messageID) {
			this.messageID = messageID;
			this.recipient = recipient;
		}
	}
	
	private static final class MutableInteger {
	    private int value;
	    
	    public MutableInteger(int value) {
	        this.value = value;
	    }
	    
	    public void set(int value) {
	        this.value = value;
	    }
	    
	    public int intValue() {
	        return value;
	    }
	}
	
}
