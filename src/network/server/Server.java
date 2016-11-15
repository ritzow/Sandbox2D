package network.server;

import java.io.IOException;
import java.net.*;
import network.message.ServerConnectAcknowledgement;
import network.message.ServerConnectRequest;
import networkutils.DatagramInputStream;
import networkutils.DatagramOutputStream;
import networkutils.InvalidMessageException;
import util.Exitable;

public class Server implements Runnable, Exitable {

	protected volatile boolean exit, finished;
	protected DatagramSocket socket;
	protected DatagramOutputStream[] clients;

	public Server() {
		clients = new DatagramOutputStream[20];
	}
	
	public Server(short capacity) {
		clients = new DatagramOutputStream[capacity];
	}

	@Override
	public void run() {
		try {
			socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 50000));
			DatagramInputStream input = new DatagramInputStream(socket);
			
			while(!exit) {
				DatagramPacket packet = new DatagramPacket(new byte[100], 100);
				socket.receive(packet);
				System.out.println("Server received message");
				try {
					new ServerConnectRequest(packet.getData());
				} catch(InvalidMessageException e) {
					System.out.println("Invalid message");
					continue;
				}
				
				byte[] response = new ServerConnectAcknowledgement(true).getBytes();
				socket.send(new DatagramPacket(response, response.length, packet.getSocketAddress()));
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

	public int addClient(DatagramOutputStream output) {
		for(int i = 0; i < clients.length; i++) {
			if(clients[i] == null) {
				clients[i] = output;
				return i;
			}
		}

		return -1;
	}

	public void removeClient(DatagramOutputStream output) {
		for(int i = 0; i < clients.length; i++) {
			if(clients[i] == output) {
				clients[i] = null;
			}
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
