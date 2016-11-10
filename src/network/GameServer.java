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
import networkutils.Message;
import networkutils.UnknownMessageException;
import util.Exitable;
import world.World;
import world.WorldManager;

public class GameServer implements Runnable, Exitable {
	
	protected volatile boolean exit, finished;
	
	protected DatagramSocket socket;
	
	protected DatagramOutputStream[] clients;
	
	public GameServer(short capacity) {
		clients = new DatagramOutputStream[capacity];
	}

	@Override
	public void run() {
		//TODO continue creating game server
		try {
			socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 50000));
			DatagramInputStream input = new DatagramInputStream(socket);
			while(!exit) {
				byte[] data = input.readPacket();
				Message message = MessageParser.getMessage(data);
				if(message instanceof ServerInfoRequest) {
					DatagramOutputStream output = new DatagramOutputStream(socket, input.getBuffer().getSocketAddress()); //will keep this in real situation
					output.write(new ServerInfoMessage((short)24, (short)clients.length).getBytes()); //test amount
					output.close();
				}
				
				else if(message instanceof ServerConnectRequest) {
					int index = addClient(new DatagramOutputStream(socket, input.getBuffer().getSocketAddress()));
					clients[index].write(new ServerConnectAcknowledgement(true).getBytes());
				}
				
				else if(message instanceof ClientInfoMessage) {
					System.out.println(message);
				}
				
				else {
					System.out.println("Message:" + message);
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
	
	protected WorldManager startWorld(World world) {
		WorldManager worldManager = new WorldManager(world);
		new Thread(worldManager, "World " + world.hashCode()).start();
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
