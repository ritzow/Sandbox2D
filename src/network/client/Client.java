package network.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import network.message.*;
import util.Exitable;

public final class Client implements Closeable, Runnable, Exitable {

	protected volatile boolean exit, finished;
	protected final DatagramSocket socket;
	protected SocketAddress serverAddress;
	
	protected volatile LinkedList<DatagramPacket> unprocessedPackets;
	
	public Client() throws IOException {
		unprocessedPackets = new LinkedList<DatagramPacket>();
		socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0));
	}
	
	/**
	 * Connect to a specified SocketAddress using a ServerConnectRequest
	 * @param address the socket address of the server
	 * @param attempts number of times to resend the connection request
	 * @param timeout the total amount of time in milliseconds to wait for the server to respond
	 * @return whether or not the server responded and accepted the client's connection
	 * @throws IOException if the socket throws an IOException
	 */
	public synchronized boolean connectToServer(SocketAddress address, int attempts, int timeout) throws IOException {
		if(timeout != 0 && timeout < attempts)
			throw new UnsupportedOperationException("timeout too small");
		socket.setSoTimeout(timeout/attempts);
		DatagramPacket response = new DatagramPacket(new byte[10], 10);
		ServerConnectRequest request = new ServerConnectRequest();
		send(request, address);
		long startTime = System.currentTimeMillis();
		int attemptsRemaining = attempts;
		while(attemptsRemaining >= 0 && (System.currentTimeMillis() - startTime < timeout || timeout == 0)) {
			try {
				socket.receive(response);
				if(response.getSocketAddress().equals(address)) {
					if(new ServerConnectAcknowledgment(response).isAccepted()) {
						serverAddress = response.getSocketAddress();
						return true;
					} else {
						return false;
					}
				} else {
					continue;
				}
			} catch(InvalidMessageException e) {
				continue;
			} catch(PortUnreachableException e) {
				return false;
			} catch(SocketTimeoutException e) {
				if(--attemptsRemaining > 0)
					send(request, address);
				continue;
			}
		}
		return false;
	}
	
	public synchronized void send(Message message, SocketAddress address) throws IOException {
		send(message.getBytes(), address);
	}
	
	protected synchronized void send(byte[] data, SocketAddress address) throws IOException {
		socket.send(new DatagramPacket(data, data.length, address));
	}
	
	@Override
	public void close() throws IOException {
		socket.close();
		this.exit = true;
	}
	
	@Override
	public void run() {
		if(serverAddress == null)
			throw new UnsupportedOperationException("Client has not connected to a server");
		
		try {
			socket.setSoTimeout(0);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		MessageHandler messageHandler = new MessageHandler() {
			@Override
			public void handle(ServerInfo message, SocketAddress sender) {
				System.out.println(message);
			}
			
			@Override
			public void handle(WorldCreationMessage message, SocketAddress sender) {
				System.out.println(message);
			}
		};
		
		ExecutorService scheduler = Executors.newCachedThreadPool();
		DatagramPacket buffer = new DatagramPacket(new byte[1024], 1024);
		while(!exit) {
			try {
				socket.receive(buffer);
				byte[] packetData = new byte[buffer.getLength()]; //TODO this stuff is pretty unoptimized, perhaps use byte arrays in packet processing? VVV
				System.arraycopy(buffer.getData(), buffer.getOffset(), packetData, 0, packetData.length);
				DatagramPacket packet = new DatagramPacket(packetData, 0, packetData.length, buffer.getAddress(), buffer.getPort());
				scheduler.execute(new Runnable() {
					public void run() {
						try {
							Protocol.processPacket(packet, messageHandler);
						} catch (UnknownMessageException e) {
							return;
						} catch (InvalidMessageException e) {
							return;
						}
					}
				});
			} catch(SocketException e) {
				if(!socket.isClosed()) {
					e.printStackTrace();
				} else {
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		synchronized(this) {
			finished = true;
			this.notifyAll();
		}
	}
	
	protected synchronized void add(DatagramPacket packet) {
		synchronized(unprocessedPackets) {
			unprocessedPackets.add(packet);	
		}
	}

	@Override
	public synchronized void exit() {
		try {
			close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean isFinished() {
		return finished;
	}
}
