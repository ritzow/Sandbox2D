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

/** Provides common functionality of the client and server. Manages incoming and outgoing packets. **/
public abstract class NetworkController implements Installable, Runnable, Exitable {
	private volatile boolean setupComplete, exit, finished;
	private final DatagramSocket socket;
	private final List<MessageAddressPair> reliable;
	
	public NetworkController(SocketAddress bindAddress) throws SocketException {
		socket = new DatagramSocket(bindAddress);
		reliable = new LinkedList<MessageAddressPair>();
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
		reliableSend(packet, address, 500);
	}
	
	public void reliableSend(byte[] packet, SocketAddress address, long resendInterval) {
		if(packet.length < 6)
			throw new RuntimeException("invalid packet");
		DatagramPacket datagram = new DatagramPacket(packet, packet.length, address); 
		MessageAddressPair pair = new MessageAddressPair(ByteUtil.getInteger(packet, 0), address);
		reliable.add(pair);
		synchronized(pair) {
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
	
	public void send(byte[][] packets, SocketAddress address) {
		for(byte[] a : packets) {
			send(a, address);
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
		ExecutorService dispatcher = Executors.newFixedThreadPool(10);
		DatagramPacket buffer = new DatagramPacket(new byte[Protocol.MAX_MESSAGE_LENGTH], Protocol.MAX_MESSAGE_LENGTH);
		
		synchronized(this) {
			setupComplete = true;
			this.notifyAll();
		}

		while(!exit) {
			try {
				socket.receive(buffer);
				if(buffer.getLength() < 6)
					continue; //ignore received messages that are too short
				
				int messageID = ByteUtil.getInteger(buffer.getData(), buffer.getOffset());
				short protocol = ByteUtil.getShort(buffer.getData(), buffer.getOffset() + 4);
				byte[] data = Arrays.copyOfRange(buffer.getData(), 6, buffer.getLength());
				SocketAddress sender = buffer.getSocketAddress();
			
				//notify reliableSend that the recipient received the message
				if(protocol == Protocol.RESPONSE_MESSAGE) {
					int responseMessageID = ByteUtil.getInteger(data, 0);
					Iterator<MessageAddressPair> iterator = reliable.iterator();
					while(iterator.hasNext()) { //TODO account for messages already received, perhaps using reliable ordered messages and storing last received
						MessageAddressPair pair = iterator.next();
						if(pair.getMessageID() == responseMessageID && pair.getRecipient().equals(sender)) {
							synchronized(pair) {
								pair.setReceived();
								pair.notifyAll();	
							}
							iterator.remove();
						}
					}
					continue; //dont process response messages
				}
				
				if(Protocol.isReliable(protocol)) {
					send(Protocol.constructMessageResponse(0, messageID), sender);
				}
				
				/* Creates a copy of the SocketAddress and data so they can be used on the processor's thread,
				 * the messageID and protocol are local variables only so they don't need to be copied.
				 * Executes the processMessage method on another thread. */
				dispatcher.execute(() -> process(messageID, protocol, sender, data));
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
}
