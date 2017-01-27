package ritzow.solomon.engine.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import ritzow.solomon.engine.world.World;
import ritzow.solomon.engine.world.WorldUpdater;

public class Server extends NetworkController {
	protected WorldUpdater worldUpdater;
	protected final ClientState[] clients;
	protected int localMessageID;
	
	public Server() throws SocketException, UnknownHostException {
		this(20);
	}
	
	public Server(int maxClients) throws SocketException, UnknownHostException {
		super(new InetSocketAddress(InetAddress.getLocalHost(), 50000));
		clients = new ClientState[maxClients];
	}
	
	protected void process(int messageID, short protocol, SocketAddress sender, byte[] data) {
		System.out.println("Server received message of ID " + messageID + " and type " + protocol);
		
		ClientState client = forAddress(sender);
		
		if(client != null) {
			if(protocol == Protocol.CLIENT_DISCONNECT) {
				removeClient(forAddress(sender));
			}
		} else {
			if(protocol == Protocol.SERVER_CONNECT_REQUEST) {
				//determine if client can connect, and send a response
				boolean canConnect = client == null && clientsConnected() < clients.length;
				reliableSend(Protocol.constructServerConnectResponse(canConnect), sender);
				
				//add the client to the server and send the world to the client if there is one
				if(canConnect) {
					ClientState newClient = new ClientState(sender);
					addClient(newClient);
					if(worldUpdater != null & !worldUpdater.isFinished()) {
						byte[][] worldPackets = Protocol.constructWorldPackets(1, worldUpdater.getWorld());
						newClient.lastMessageID += worldPackets.length;
						for(byte[] a : worldPackets) {
							reliableSend(a, sender);
						}
					}
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
	
	public void send(byte[] packet, SocketAddress address) {
		super.send(packet, address);
		ClientState client = forAddress(address);
		if(client != null)
			client.lastMessageID++;
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
	
	protected ClientState forAddress(SocketAddress address) {
		for(ClientState client : clients) {
			if(client != null && client.getAddress().equals(address)) {
				return client;
			}
		}
		return null;
	}
	
	/**
	 * Checks to see if the specified SocketAddress is already connected to the server
	 * @param address the SocketAddress to query
	 * @return whether or not the address specified is present
	 */
	protected boolean addressPresent(SocketAddress address) {
		for(ClientState client : clients) {
			if(client != null && client.getAddress().equals(address)) {
				return true;
			}
		}
		
		return false;
	}

	protected boolean addClient(ClientState client) {
		for(int i = 0; i < clients.length; i++) {
			if(clients[i] == null) {
				clients[i] = client;
				return true;
			}
		}
		return false;
	}

	protected boolean removeClient(ClientState client) {
		for(int i = 0; i < clients.length; i++) {
			if(clients[i] == client) {
				clients[i] = null;
				return true;
			}
		}
		return false;
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
