package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import util.Exitable;
import util.Installable;

public class Server implements Runnable, Installable, Exitable {

	private volatile boolean exit;
	private volatile boolean setupComplete;
	private volatile boolean finished;
	private int port;
	
	private ArrayList<Socket> connections;
	private ArrayList<MessageHandler> listeners;
	private volatile ServerSocket connectionListener;
	
	public Server(int port) {
		this.port = port;
		connections = new ArrayList<Socket>();
		listeners = new ArrayList<MessageHandler>();
	}

	@Override
	public void run() {
		new Thread("Server Connection Listener") {
			public void run() {
				try {
					connectionListener = new ServerSocket(port);

					try {
						while(!exit) {
							Socket newConnection = connectionListener.accept();
							
							if(newConnection != null) {
								synchronized(connections) {
									connections.add(newConnection);
									MessageHandler listener = new MessageHandler(newConnection);
									new Thread(listener, "Message Listener " + listener.hashCode()).start();
									listeners.add(listener);
								}
							}
						}
					} catch(SocketException e) {
						//server socket is closed while waiting for an incoming connection
					}
					
					connectionListener.close();
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
		
		new Thread("Server Input Listener") {
			public void run() {
				while(!exit) {
					for(int i = 0; i < connections.size(); i++) {
						//TODO put all of this stuff on a separate thread per client (REMOVE THIS THREAD)
						
					}
				}
			}
		}.start();
		
		synchronized(this) {
			setupComplete = true;
			this.notifyAll();
		}
		
		while(!exit) {
			for(int i = 0; i < connections.size(); i++) {
				synchronized(connections) {
					//TODO send data to clients
				}
			}
		}
		
		try {
			for(int i = 0; i < connections.size(); i++) {
				connections.get(i).close();
			}
			connectionListener.close();
		} catch (IOException e) {
			
		}
		
		synchronized(this) {
			this.finished = true;
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

	@Override
	public synchronized boolean isSetupComplete() {
		return setupComplete;
	}
}
