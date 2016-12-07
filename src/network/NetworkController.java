package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import network.message.InvalidMessageException;
import network.message.MessageHandler;
import network.message.Protocol;
import network.message.UnknownMessageException;
import util.Exitable;
import util.Installable;

/**
 * @author Solomon Ritzow
 *
 */
public abstract class NetworkController implements Installable, Runnable, Exitable {
	protected volatile boolean setupComplete, exit, finished;
	protected volatile MessageHandler messageHandler;
	protected final DatagramSocket socket;
	private final BlockingQueue<DatagramPacket> unsent;

	public NetworkController(SocketAddress localAddress) throws SocketException {
		socket = new DatagramSocket(localAddress);
		unsent = new LinkedBlockingQueue<DatagramPacket>();
	}
	
	public void send(DatagramPacket packet) {
		unsent.add(packet);
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
		if(messageHandler == null)
			throw new RuntimeException("Server has no message handler");
		
		Thread sender = new Thread() {
			public void run() {
				while(!exit) {
					try {
						socket.send(unsent.take()); //wait for a packet to be queued and send it
					} catch (InterruptedException | SocketException e) {
						return; //exit if interrupted while waiting
					} catch (IOException e) {
						continue;
					}
				}
			}
		};
		
		sender.start();
		
		ExecutorService processor = Executors.newCachedThreadPool();
		DatagramPacket buffer = new DatagramPacket(new byte[1024], 1024);
		
		synchronized(this) {
			setupComplete = true;
			this.notifyAll();
		}
		
		while(!exit) {
			try {
				socket.setSoTimeout(0);
				socket.receive(buffer);
				byte[] packetData = new byte[buffer.getLength()];
				System.arraycopy(buffer.getData(), buffer.getOffset(), packetData, 0, packetData.length);
				DatagramPacket packet = new DatagramPacket(packetData, 0, packetData.length, buffer.getAddress(), buffer.getPort());
				processor.execute(new Runnable() {
					public void run() {
						try {
							Protocol.processPacket(packet, messageHandler);
						} catch (UnknownMessageException | InvalidMessageException e) {
							return;
						}
					}
				});
			} catch(SocketException e) {
				if(!socket.isClosed()) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		processor.shutdown();
		
		try {
			processor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		sender.interrupt();
		socket.close();

		synchronized(this) {
			finished = true;
			this.notifyAll();
		}
	}
}
