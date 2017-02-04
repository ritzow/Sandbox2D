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
	private final List<MessageAddressPair> reliable;
	private final Map<SocketAddress, MutableInteger> lastReceived;
	private final short[] reliableProtocols;
	
	public NetworkController(SocketAddress bindAddress, short[] reliableProtocols) throws SocketException {
		socket = new DatagramSocket(bindAddress);
		reliable = new LinkedList<MessageAddressPair>();
		lastReceived = new HashMap<SocketAddress, MutableInteger>();
		this.reliableProtocols = reliableProtocols;
		Arrays.sort(reliableProtocols);
	}
	
	public void send(byte[] packet, SocketAddress address) {
		if(packet.length < 6)
			throw new RuntimeException("invalid packet");
		try {
			DatagramPacket datagram = new DatagramPacket(packet, packet.length, address); 
			socket.send(datagram);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void reliableSend(byte[] packet, SocketAddress address) {
		reliableSend(packet, address, 100);
	}
	
	public void reliableSend(byte[] packet, SocketAddress address, long resendInterval) {
		if(packet.length < 6)
			throw new RuntimeException("invalid packet");
		DatagramPacket datagram = new DatagramPacket(packet, packet.length, address); 
		MessageAddressPair pair = new MessageAddressPair(ByteUtil.getInteger(packet, 0), address);
		
		synchronized(pair) {
			synchronized(reliable) {
				reliable.add(pair);
			}
			
			try {
				while(!pair.getReceived()) {
					socket.send(datagram);
					pair.wait(resendInterval);
				}
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}
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
	protected abstract void process(int messageID, short protocol, SocketAddress sender, byte[] data);
	
	@Override
	public void run() {
		ExecutorService dispatcher = Executors.newCachedThreadPool();
		DatagramPacket buffer = new DatagramPacket(new byte[Protocol.MAX_MESSAGE_LENGTH], Protocol.MAX_MESSAGE_LENGTH);
		
		synchronized(this) {
			setupComplete = true;
			this.notifyAll();
		}

		while(!exit) {
			try {
				//wait for a packet to be received
				socket.receive(buffer);
				
				//ignore received packets that are too short
				if(buffer.getLength() < 6)
					continue;
				
				//parse the packet information
				final int messageID = ByteUtil.getInteger(buffer.getData(), buffer.getOffset());
				final short protocol = ByteUtil.getShort(buffer.getData(), buffer.getOffset() + 4);
				final byte[] data = Arrays.copyOfRange(buffer.getData(), 6, buffer.getLength());
				final SocketAddress sender = buffer.getSocketAddress();
				
				if(protocol == Protocol.RESPONSE_MESSAGE) {
					int responseMessageID = ByteUtil.getInteger(data, 0);
					synchronized(reliable) {
						Iterator<MessageAddressPair> iterator = reliable.iterator();
						while(iterator.hasNext()) {
							MessageAddressPair pair = iterator.next();
							if(pair.getMessageID() == responseMessageID && pair.getRecipient().equals(sender)) {
								synchronized(pair) {
									pair.setReceived();
									pair.notifyAll();
								}
								iterator.remove();
							}
						}
					}
				} else if(Arrays.binarySearch(reliableProtocols, protocol) >= 0) { //handle reliable messages by first checking if the received message is reliable
					if(!lastReceived.containsKey(sender)) { //if sender isn't registered yet, add it to hashmap, if the ack isn't received, it will be resent on next send
						lastReceived.put(sender, new MutableInteger(messageID));
						dispatcher.execute(() -> process(messageID, protocol, sender, data));
						send(Protocol.constructMessageResponse(messageID), sender);
					} else if(messageID == lastReceived.get(sender).intValue() + 1) { //if the message is the next one, process it and update last message
						lastReceived.get(sender).set(messageID);
						dispatcher.execute(() -> process(messageID, protocol, sender, data));
						send(Protocol.constructMessageResponse(messageID), sender);
					} else { //if the message was already received
						send(Protocol.constructMessageResponse(messageID), sender);
						continue; //dont process the message because it isn't new
					}
				} else { //if the message isnt a response and isn't reliable, process it!
					dispatcher.execute(() -> process(messageID, protocol, sender, data));
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
