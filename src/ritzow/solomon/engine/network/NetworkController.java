package ritzow.solomon.engine.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import ritzow.solomon.engine.network.message.MessageProcessor;
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
	protected volatile MessageProcessor processor;
	protected final DatagramSocket socket;
	protected final List<Integer> handles;
	
	public NetworkController(SocketAddress bindAddress) throws SocketException {
		handles = new ArrayList<Integer>();
		socket = new DatagramSocket(bindAddress);
	}
	
	public void setTimeout(int milliseconds) throws SocketException { //TODO find different solution in client connect method
		socket.setSoTimeout(milliseconds);
	}
	
	public void send(byte[] packet, SocketAddress address) throws IOException {
		socket.send(new DatagramPacket(packet, packet.length, address));
	}
	
	public Integer sendReliable(byte[] packet, SocketAddress address) throws IOException {
		send(packet, address);
		Integer id = new Integer(ByteUtil.getInteger(packet, 0)); 
		handles.add(id);
		return id;
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
		if(processor == null)
			throw new RuntimeException("Server has no message handler");
		
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
				
				dispatcher.execute(new Runnable() {
					//store the packet data because it wont be available once the socket calls receive again (which happens immediately)
					byte[] packet = Arrays.copyOfRange(buffer.getData(), buffer.getOffset(), buffer.getOffset() + buffer.getLength());
					SocketAddress address = buffer.getSocketAddress();
					
					public void run() {
						int messageID = ByteUtil.getInteger(packet, 0);
						short protocol = ByteUtil.getShort(packet, 4);
						processor.processMessage(messageID, protocol, address, Arrays.copyOfRange(packet, 6, packet.length));
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
