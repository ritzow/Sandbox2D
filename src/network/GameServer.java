package network;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import networkutils.DatagramInputStream;
import networkutils.DatagramOutputStream;
import util.Exitable;

public class GameServer implements Runnable, Exitable {

	protected volatile boolean exit, finished;
	protected DatagramSocket socket;
	protected DatagramOutputStream[] clients;

	public GameServer(short capacity) {
		clients = new DatagramOutputStream[capacity];
	}

	@Override
	public void run() {
		try {
			socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 50000));
			DatagramInputStream input = new DatagramInputStream(socket);
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
