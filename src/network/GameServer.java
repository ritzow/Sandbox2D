package network;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import networkutils.DatagramInputStream;
import util.Exitable;
import world.World;
import world.WorldManager;

public class GameServer implements Runnable, Exitable {
	
	protected volatile boolean exit, finished;
	
	protected DatagramSocket socket;

	@Override
	public void run() {
		
		try {
			socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 50000));
			DatagramInputStream input = new DatagramInputStream(socket);
			@SuppressWarnings("unused")
			byte[] data;
			while(!exit) {
				data = input.readPacket();
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
	
	protected WorldManager startWorld(World world) {
		WorldManager worldManager = new WorldManager(world);
		new Thread(worldManager).start();
		return worldManager;
	}

	@Override
	public void exit() {
		this.exit = true;
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

}
