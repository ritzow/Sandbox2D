package network;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import util.Exitable;

public final class Client implements Runnable, Exitable {
	
	private volatile boolean exit;
	private boolean finished;
	
	private InetAddress address;
	private int port;
	
	private Socket connection;
	
	public Client(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}
	
	@Override
	public void run() {
		try {
			connection = new Socket(address, port);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch(ConnectException e) {
			System.err.println("Client could not connect to " + address.getCanonicalHostName() + " on port " + port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		new Thread("Client Input Listener") {
			public void run() {
				while(!exit) {
					//TODO read messages sent from server and process
				}
			}
		}.start();
		
		while(!exit) {
			//TODO send data to server, perhaps have thread wait until there is data to send?
		}
	}
	
	public void sendData(byte[] data) {
		
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
