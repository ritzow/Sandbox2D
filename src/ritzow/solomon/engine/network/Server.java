package ritzow.solomon.engine.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import ritzow.solomon.engine.world.World;
import ritzow.solomon.engine.world.WorldUpdater;

public class Server extends NetworkController {
	protected WorldUpdater worldUpdater;
	protected final SocketAddress[] clients;
	
	public Server() throws SocketException, UnknownHostException {
		this(20);
	}
	
	public Server(int maxClients) throws SocketException, UnknownHostException {
		super(new InetSocketAddress(InetAddress.getLocalHost(), 50000));
		clients = new SocketAddress[maxClients];
	}
	
	protected void processMessage(int messageID, short protocol, SocketAddress sender, byte[] data) {
		System.out.println("Server received message of ID " + messageID + " and type " + protocol);
		
		if(clientPresent(sender)) {
			if(protocol == Protocol.CLIENT_DISCONNECT) {
				removeClient(sender);
			}
		} else {
			if(protocol == Protocol.SERVER_CONNECT_REQUEST) {
				try {
					boolean canConnect = clientsConnected() < clients.length;
					send(Protocol.constructServerConnectResponse(canConnect), sender);
					
					if(worldUpdater != null & !worldUpdater.isFinished()) {
						byte[][] world = Protocol.constructWorldPackets(1, worldUpdater.getWorld());
						for(byte[] a : world) {
							send(a, sender);
						}
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public void exit() {
		try {
			stopWorld();
		} catch(RuntimeException e) {
			//ignores "no world to stop" exception
		} finally {
			super.exit();
		}
	}
	
	public void startWorld(World world) {
		if(worldUpdater == null || worldUpdater.isFinished())
			new Thread(worldUpdater = new WorldUpdater(world), "Server World Updater").start();
		else
			throw new RuntimeException("A world is already running");
	}
	
	public void stopWorld() {
		if(worldUpdater != null && !worldUpdater.isFinished())
			worldUpdater.exit();
		else
			throw new RuntimeException("There is no world currently running");
	}
	
	public World getWorld() {
		return worldUpdater == null ? null : worldUpdater.getWorld();
	}
	
	/**
	 * Checks to see if the specified SocketAddress is already connected to the server
	 * @param address the SocketAddress to query
	 * @return whether or not the address specified is present
	 */
	protected boolean clientPresent(SocketAddress address) {
		for(int i = 0; i < clients.length; i++) {
			if(clients[i] != null && clients[i].equals(address)) {
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
			if(clients[i] != null && clients[i].equals(output)) {
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
}
