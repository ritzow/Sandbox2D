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

/**
 * Provides common functionality of the client and server. Manages incoming and outgoing packets.
 * @author Solomon Ritzow
 *
 */
public abstract class NetworkController implements Installable, Runnable, Exitable {
	private volatile boolean setupComplete, exit, finished;
	protected final DatagramSocket socket;
	private final List<MessageHandle> handles;
	
	public NetworkController(SocketAddress bindAddress) throws SocketException {
		handles = new LinkedList<MessageHandle>();
		socket = new DatagramSocket(bindAddress);
	}
	
	protected abstract void processMessage(int messageID, short protocol, SocketAddress sender, byte[] data);
	
	public void send(byte[] packet, SocketAddress address) throws IOException {
		if(packet.length < 6)
			throw new RuntimeException("invalid packet");
		DatagramPacket datagram = new DatagramPacket(packet, packet.length, address);
		if(Protocol.isReliable(ByteUtil.getShort(packet, 4))) {
			if(!this.isSetupComplete() || this.isFinished()) {
				throw new RuntimeException("the network controller must be running to send packets reliably");	
			} else {
				try {
					MessageHandle handle = new MessageHandle(ByteUtil.getInteger(packet, 0));
					handles.add(handle);
					
					do {
						socket.send(datagram);
					} while(!handle.waitForResponse());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} else {
			socket.send(datagram);
		}
	}
	
	public void send(byte[][] packets, SocketAddress address) throws IOException {
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
	
	@Override
	public void run() {
		ExecutorService dispatcher = Executors.newFixedThreadPool(10); //TODO experiment with thread pool types
		DatagramPacket buffer = new DatagramPacket(new byte[Protocol.MAX_MESSAGE_LENGTH], Protocol.MAX_MESSAGE_LENGTH);
		List<Integer> received = new ArrayList<Integer>();
		
		synchronized(this) {
			setupComplete = true;
			this.notifyAll();
		}

		while(!exit) {
			try {
				socket.setSoTimeout(0);
				socket.receive(buffer);
				if(buffer.getLength() < 6)
					continue; //ignore received messages that are too short
				
//				if(Math.random() < 0.5) //currently set at 50% chance that message is not received
//					continue;

				final int messageID = ByteUtil.getInteger(buffer.getData(), buffer.getOffset());
				final short protocol = ByteUtil.getShort(buffer.getData(), buffer.getOffset() + 4);
				
				if(protocol == Protocol.RESPONSE_MESSAGE) { //notify any send call waiting for this response
					int receivedID = ByteUtil.getInteger(buffer.getData(), buffer.getOffset() + 6); //message id that the received message is a response to
					
					for(MessageHandle h : handles) {
						if(receivedID == h.getMessageID()) {
							h.notifyReceived();
							break;
						}
					}
					
					//TODO remove old message handles from list
					
				} else {
					if(Protocol.isReliable(protocol)) { //responsd to reliable messages
						if(received.contains(messageID)) {
							send(Protocol.constructMessageResponse(0, messageID), buffer.getSocketAddress()); continue; //don't reprocess packet
						} else {
							received.add(messageID);
						}
						//TODO remove old received message IDs from list
					}
					
					/* Creates a copy of the SocketAddress and data so they can be used on the processor's thread,
					 * the messageID and protocol are local variables only so they don't need to be copied.
					 * Executes the processMessage method on another thread. */
					SocketAddress sender = buffer.getSocketAddress();
					byte[] data = Arrays.copyOfRange(buffer.getData(), 6, buffer.getLength());
					dispatcher.execute(() -> processMessage(messageID, protocol, sender, data));
				}
			} catch(SocketTimeoutException e) {
				continue;
			} catch(SocketException e) {
				if(!socket.isClosed()) {
					e.printStackTrace();
				}
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
