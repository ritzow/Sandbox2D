package ritzow.solomon.engine.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import ritzow.solomon.engine.network.message.MessageHandle;
import ritzow.solomon.engine.network.message.Protocol;
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
	
	public void setTimeout(int milliseconds) throws SocketException { //TODO find different solution in client connect method
		socket.setSoTimeout(milliseconds);
	}
	
	public void send(byte[] packet, SocketAddress address) throws IOException {
		socket.send(new DatagramPacket(packet, packet.length, address));
	}
	
	public void sendReliable(byte[] packet, SocketAddress address) throws IOException {
		if(packet.length < 4)
			throw new RuntimeException("invalid packet");
		else if(!this.isSetupComplete() || this.isFinished())
			throw new RuntimeException("the network controller must be running to send packets reliably");
		try {
			
			MessageHandle handle = new MessageHandle(ByteUtil.getInteger(packet, 0));
			handles.add(handle);
			send(packet, address);
			
			do {
				send(packet, address);
			} while(!handle.waitForResponse(500));
		} catch (InterruptedException e) {
			e.printStackTrace();
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
		ExecutorService dispatcher = Executors.newCachedThreadPool();
		DatagramPacket buffer = new DatagramPacket(new byte[Protocol.MAX_MESSAGE_LENGTH], Protocol.MAX_MESSAGE_LENGTH);
		
		synchronized(this) {
			setupComplete = true;
			this.notifyAll();
		}

		while(!exit) {
			try {
				socket.setSoTimeout(0);
				socket.receive(buffer);
				if(buffer.getLength() < 6)
					continue; //it's a troll, just ignore it.
				
				int protocol = ByteUtil.getShort(buffer.getData(), buffer.getOffset() + 4);
				
				if(protocol == Protocol.RESPONSE_MESSAGE) {
					int messageID = ByteUtil.getInteger(buffer.getData(), buffer.getOffset() + 6); //message id that the received message is a response to
					
					ListIterator<MessageHandle> iterator = handles.listIterator();
					while(iterator.hasNext()) {
						MessageHandle h = iterator.next();
						if(h.getMessageID() == messageID) {
							h.notifyReceived();
							iterator.remove(); //TODO this doesn't account for a timeout, message handles will stay in the list
							break;
						}
					}
				}
				
				dispatcher.execute(new Runnable() {
					//store the packet data because it wont be available once the socket calls receive again (which happens immediately)
					byte[] packet = Arrays.copyOfRange(buffer.getData(), buffer.getOffset(), buffer.getOffset() + buffer.getLength());
					SocketAddress address = buffer.getSocketAddress();
					
					public void run() {
						processMessage(ByteUtil.getInteger(packet, 0), ByteUtil.getShort(packet, 4), address, Arrays.copyOfRange(packet, 6, packet.length));
					}
				});
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
