package network.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import network.MessageHandler;
import network.message.*;
import networkutils.DatagramInputStream;
import util.ByteUtil;
import util.Exitable;

public class Server implements Runnable, Exitable, Closeable {

	protected volatile boolean exit, finished;
	protected DatagramSocket socket;
	protected SocketAddress[] clients;
	protected MessageHandler messageHandler;

	public Server() {
		clients = new SocketAddress[10];
	}
	
	public Server(short capacity) {
		clients = new SocketAddress[capacity];
	}

	@Override
	public void run() {
		try {
			socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 50000));
			DatagramInputStream input = new DatagramInputStream(socket);
			
			messageHandler = new MessageHandler() {

				@Override
				public void handle(ServerConnectRequest messsage, SocketAddress sender) {
					try {
						sendData(new ServerConnectAcknowledgment(clientsConnected() < clients.length).getBytes(), sender);
						addClient(sender);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void handle(ServerInfoRequest message, SocketAddress sender) {
					
				}
				
				@Override
				public void handle(ClientInfoMessage message, SocketAddress sender) {
					
				}
			};
			
			while(!exit) {
				DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
				socket.receive(packet);
				
				try {
					processPacket(packet);
				} catch (InvalidMessageException e) {
					continue;
				} catch (UnknownMessageException e) {
					continue;
				}
			}
			
			input.close();
		} catch(SocketException e) {
			
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			socket.close();
		}

		synchronized(this) {
			finished = true;
			this.notifyAll();
		}
	}
	
	public synchronized void sendData(byte[] data, SocketAddress address) throws IOException {
		socket.send(new DatagramPacket(data, data.length, address));
	}
	
	@Override
	public synchronized void close() throws IOException {
		socket.close();
		exit();
	}
	
	protected boolean clientPresent(SocketAddress address) {
		for(int i = 0; i < clients.length; i++) {
			if(clients[i].equals(address)) {
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
			if(clients[i].equals(output)) {
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
