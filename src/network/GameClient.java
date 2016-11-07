package network;

import java.net.*;
import util.Exitable;

public class GameClient implements Runnable, Exitable {
	
	protected DatagramSocket socket;
	
	protected volatile boolean exit;
	protected volatile boolean finished;
	
	public GameClient() {
		
	}
	
	@Override
	public void run() {
		try {
			socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0));
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		synchronized(this) {
			finished = true;
			this.notifyAll();
		}
	}

	@Override
	public void exit() {
		this.exit = true;
	}

	@Override
	public boolean isFinished() {
		return finished;
	}
	
	public void connectToServer() {
		
	}
	
}
