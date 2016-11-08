package network;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import network.message.*;
import networkutils.DatagramInputStream;
import networkutils.DatagramOutputStream;
import networkutils.InvalidMessageException;
import networkutils.UnknownMessageException;
import util.Exitable;
import world.World;
import world.WorldManager;

public class GameServer implements Runnable, Exitable {
	
	protected volatile boolean exit, finished;
	
	protected DatagramSocket socket;
	
	protected InetSocketAddress[] clients;
	
	public GameServer(short capacity) {
		clients = new InetSocketAddress[capacity];
	}

	@Override
	public void run() {
		
		try {
			socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 50000));
			DatagramInputStream input = new DatagramInputStream(socket);
			while(!exit) {
				byte[] data = input.readPacket();
				if(MessageParser.getMessage(data) instanceof ServerInfoRequest) {
					DatagramOutputStream output = new DatagramOutputStream(socket, input.getBuffer().getSocketAddress()); //will keep this in real situation
					output.write(new ServerInfoMessage((short)24, (short)clients.length).getBytes()); //test amount
					output.close();
				}
				
				else {
					System.out.println("Unknown message: " + new String(data));
				}
			}
			
			input.close();
		} catch(SocketException e) {
			
		} catch(IOException e) {
			e.printStackTrace();
		} catch (UnknownMessageException e) {
			e.printStackTrace();
		} catch (InvalidMessageException e) {
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
