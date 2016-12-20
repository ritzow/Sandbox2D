package ritzow.solomon.engine.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import ritzow.solomon.engine.network.message.InvalidMessageException;
import ritzow.solomon.engine.network.message.Message;
import ritzow.solomon.engine.network.message.MessageHandler;
import ritzow.solomon.engine.network.message.Protocol;
import ritzow.solomon.engine.network.message.UnknownMessageException;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.util.Exitable;
import ritzow.solomon.engine.util.Installable;

/**
 * Provides common functionality of a client or server. Manages incoming and outgoing packets.
 * @author Solomon Ritzow
 *
 */
public abstract class NetworkController implements Installable, Runnable, Exitable {
	protected volatile boolean setupComplete, exit, finished;
	protected volatile MessageHandler messageHandler;
	protected final DatagramSocket socket;
	private final BlockingQueue<Entry<Message, SocketAddress>> unsent;
	
	private final DatagramPacket sendBuffer;

	public NetworkController(SocketAddress bindAddress) throws SocketException {
		socket = new DatagramSocket(bindAddress);
		unsent = new LinkedBlockingQueue<Entry<Message, SocketAddress>>();
		sendBuffer = new DatagramPacket(new byte[0], 0);
	}
	
	public void send(Message message, SocketAddress address) {
		unsent.add(new SimpleImmutableEntry<Message, SocketAddress>(message, address));
	}
	
	public void send(byte[] packet, SocketAddress address) throws IOException {
		sendBuffer.setData(packet);
		sendBuffer.setSocketAddress(address);
		socket.send(sendBuffer);
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
						Entry<Message, SocketAddress> entry = unsent.take();
						DatagramPacket packet = Protocol.construct(0, entry.getKey(), entry.getValue()); //TODO implement message ID system
						socket.send(packet); //wait for a packet to be queued and send it
						if(entry.getKey().isReliable()) {
							//TODO 
							//how do I get incoming packets here?
						}
						
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
		DatagramPacket buffer = new DatagramPacket(new byte[Protocol.MAX_MESSAGE_LENGTH], Protocol.MAX_MESSAGE_LENGTH);
		
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
