package network.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import network.message.*;
import util.Exitable;
import world.World;

	protected final SocketAddress[] clients;
	
	protected World world;
	}
	
	public Server(int capacity) throws SocketException, UnknownHostException {
		socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 50000));
		clients = new SocketAddress[capacity];
	}

	@Override
			
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
}
