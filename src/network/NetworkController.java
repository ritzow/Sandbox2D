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
import util.ByteUtil;
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
	
	public void sendReliable(DatagramPacket packet) {
		//TODO implement message ID system for sequential message receiving and for "message <ID> received responses"
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
		
		Thread sender = new Thread("Packet Sender") {
			public void run() {
				while(!exit) {
					try {
						socket.send(unsent.take()); //wait for a packet to be queued and send it
					} catch (InterruptedException | SocketException e) {
						return; //exit if interrupted while waiting (such as when the network controller exits)
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
				if(buffer.getLength() < 6) //it's a troll, just ignore it.
					continue;
				
				//copy only the contents of the message and pass the parsed id, protocol, and data to the packet processor.
				int messageID = ByteUtil.getInteger(buffer.getData(), buffer.getOffset());
				short messageProtocol = ByteUtil.getShort(buffer.getData(), buffer.getOffset() + 4);
				byte[] data = new byte[buffer.getLength() - 6];
				System.arraycopy(buffer.getData(), buffer.getOffset() + 6, data, 0, data.length);
				processor.execute(new Runnable() {
					SocketAddress address = buffer.getSocketAddress(); //store the address because it wont be available once the socket calls receive
					public void run() {
						try {
							Protocol.process(messageID, messageProtocol, data, address, messageHandler);
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
		
		try {
			processor.shutdown();
			processor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			sender.interrupt();
			sender.join();
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
