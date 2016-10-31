package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import util.Exitable;
import util.Installable;
import util.Synchronizer;

public class Server implements Runnable, Installable, Exitable {

	private volatile boolean exit;
	private volatile boolean setupComplete;
	private volatile boolean finished;
	
	private ConnectionListener connectionListener;
	private ServerSocket socket;
	
	private ArrayList<Socket> connections;
	
	public Server(int port) {
		connections = new ArrayList<Socket>();
		try {
			this.socket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void exit() {
		this.exit = true;
	}

	@Override
	public synchronized boolean isFinished() {
		return finished;
	}

	@Override
	public synchronized boolean isSetupComplete() {
		return setupComplete;
	}

	@Override
	public void run() {
		
		new Thread(connectionListener = new ConnectionListener(socket, connections)).start();
		
		synchronized(this) {
			setupComplete = true;
			this.notifyAll();
		}
		
		while(!exit) {
			for(int i = 0; i < connections.size(); i++) {
				//TODO send data to connections?

			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {

			}
		}
		
		Synchronizer.waitForExit(connectionListener);
		
		synchronized(this) {
			this.finished = true;
			this.notifyAll();
		}
	}
	
	private class ConnectionListener implements Runnable, Exitable {
		
		private ArrayList<Socket> connections;
		private boolean exit;
		private boolean finished;
		
		private ServerSocket server;
		
		public ConnectionListener(ServerSocket server, ArrayList<Socket> connections) {
			this.connections = connections;
			this.server = server;
		}

		@Override
		public void run() {
			while(!exit) {
				try {
					connections.add(server.accept());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			synchronized(this) {
				finished = true;
				this.notifyAll();
			}
		}

		@Override
		public synchronized void exit() {
			this.exit = true;
		}

		@Override
		public synchronized boolean isFinished() {
			return finished;
		}
		
	}

}
