package network.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import network.message.*;
import util.Exitable;
import world.World;

public class Server implements Runnable, Exitable, Closeable {
	protected boolean exit, finished;
	protected final DatagramSocket socket;
	protected final SocketAddress[] clients;
	
	protected World world;

	public Server() throws SocketException, UnknownHostException {
		this(10);
	}
	
	public Server(int capacity) throws SocketException, UnknownHostException {
		socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 50000));
		clients = new SocketAddress[capacity];
	}

	@Override
	public void run() {
		final MessageHandler messageHandler = new MessageHandler() {
			@Override
			public void handle(ServerConnectRequest messsage, SocketAddress sender) {
				try {
					boolean canConnect = clientsConnected() < clients.length && !clientPresent(sender);
					if(canConnect)
						addClient(sender);
					send(new ServerConnectAcknowledgment(canConnect).getBytes(), sender);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void handle(ServerInfoRequest message, SocketAddress sender) {
				try {
					send(new ServerInfo(clientsConnected(), clients.length).getBytes(), sender);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void handle(ClientInfo message, SocketAddress sender) {
				System.out.println(message);
			}
		};
		
		ExecutorService scheduler = Executors.newCachedThreadPool();
		DatagramPacket buffer = new DatagramPacket(new byte[1024], 1024);
		while(!exit) {
			try {
				socket.receive(buffer);
				byte[] packetData = new byte[buffer.getLength()];
				System.arraycopy(buffer.getData(), buffer.getOffset(), packetData, 0, packetData.length);
				DatagramPacket packet = new DatagramPacket(packetData, 0, packetData.length, buffer.getAddress(), buffer.getPort());

				scheduler.execute(new Runnable() {
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
		
		scheduler.shutdown();
		socket.close();

		synchronized(this) {
			finished = true;
			this.notifyAll();
		}
	}
	
	public void startWorld(World world) {
		this.world = world;
	}
	
	/**
	 * Sends a message to the specified socket address
	 * @param message the message to be sent
	 * @param address the SocketAddress to send the message to
	 * @exception if the underlying call to <code>DatagramSocket.send</code> throws an IOException
	 */
	public synchronized void send(Message message, SocketAddress address) throws IOException {
		send(message.getBytes(), address);
	}
	
	/**
	 * Sends a byte array to the specified socket address
	 * @param data the byte[] to be sent
	 * @param address the SocketAddress to send the byte array to
	 * @throws IOException 
	 */
	protected synchronized void send(byte[] data, SocketAddress address) throws IOException {
		socket.send(new DatagramPacket(data, data.length, address));
	}
	
	@Override
	public void close() throws IOException {
		socket.close();
		exit();
	}
	
	protected boolean clientPresent(SocketAddress address) {
		for(int i = 0; i < clients.length; i++) {
			if(clients[i] != null && clients[i].equals(address)) {
				return true;
			}
		}
		return false;
	}

	protected boolean addClient(SocketAddress output) {
		for(int i = 0; i < clients.length; i++) {
			if(clients[i] == null) {
				clients[i] = output;
				return true;
			}
		}

		return false;
	}

	protected void removeClient(SocketAddress output) {
		for(int i = 0; i < clients.length; i++) {
			if(clients[i] != null && clients[i].equals(output)) {
				clients[i] = null;
			}
		}
	}
	
	protected int clientsConnected() {
		int connected = 0;
		for(int i = 0; i < clients.length; i++) {
			if(clients[i] != null) {
				connected++;
			}
		}
		return connected;
	}
	
	public void processPacket(DatagramPacket packet) throws UnknownMessageException, InvalidMessageException {
		if(packet.getData().length < 2)
			throw new InvalidMessageException();
		try {
			switch(ByteUtil.getShort(packet.getData(), 0)) {
			case Protocol.SERVER_CONNECT_REQUEST:
				messageHandler.handle(new ServerConnectRequest(), packet.getSocketAddress());
				return;
			case Protocol.CLIENT_INFO:
				messageHandler.handle(new ClientInfoMessage(packet), packet.getSocketAddress());
				return;
			case Protocol.SERVER_INFO:
				messageHandler.handle(new ServerInfoMessage(packet), packet.getSocketAddress());
				return;
			case Protocol.SERVER_INFO_REQUEST:
				messageHandler.handle(new ServerInfoRequest(), packet.getSocketAddress());
				return;
			case Protocol.SERVER_CONNECT_ACKNOWLEDGMENT:
				messageHandler.handle(new ServerConnectAcknowledgment(packet), packet.getSocketAddress());
			default:
				throw new UnknownMessageException();
			}
		} catch(InvalidMessageException e) {
			System.err.println("Caught invalid message");
		}

	}

	@Override
	public synchronized void exit() {
		this.exit = true;
		this.notifyAll();
	}

	@Override
	public synchronized boolean isFinished() {
		return finished;
	}

}
