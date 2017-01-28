package ritzow.solomon.engine.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import ritzow.solomon.engine.input.PlayerActions;
import ritzow.solomon.engine.util.ByteUtil;
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
		super(new InetSocketAddress(InetAddress.getLocalHost(), 50000), Protocol.getReliableProtocols());
		clients = new ClientState[maxClients];
	}
	
	protected void process(int messageID, short protocol, SocketAddress sender, byte[] data) {
		ClientState client = forAddress(sender);
		
		if(client != null) {
			if(protocol == Protocol.PLAYER_ACTION) {
				if(ByteUtil.getShort(data, 0) == PlayerActions.BLOCK_DESTROY_TEMP) {
					worldUpdater.getWorld().getForeground().destroy(worldUpdater.getWorld(), ByteUtil.getInteger(data, 2), ByteUtil.getInteger(data, 6));
				}
			}
		}
		
		else if(protocol == Protocol.SERVER_CONNECT_REQUEST) {
			//determine if client can connect, and send a response
			boolean canConnect = clientsConnected() < clients.length;
			reliableSend(Protocol.constructServerConnectResponse(1, canConnect), sender);
			
			//add the client to the server and send the world to the client if there is one
			if(canConnect) {
				ClientState newClient = new ClientState(sender);
				addClient(newClient);
				newClient.reliableMessageID = 2; //increment server's id after sending connect response
				if(worldUpdater != null & !worldUpdater.isFinished()) {
					byte[][] worldPackets = Protocol.constructWorldPackets(newClient.reliableMessageID, worldUpdater.getWorld());
					newClient.reliableMessageID += worldPackets.length;
					for(byte[] a : worldPackets) {
						reliableSend(a, sender);
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
			if(client != null && client.address.equals(address)) {
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
			if(client != null && client.address.equals(address)) {
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
