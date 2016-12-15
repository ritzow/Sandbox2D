package network.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import network.NetworkController;
import network.message.MessageHandler;
import network.message.Protocol;
import network.message.client.ServerConnectRequest;
import network.message.client.ServerInfoRequest;
import network.message.server.ServerConnectAcknowledgment;
import network.message.server.ServerInfo;
import util.Utility.Synchronizer;
import world.World;
import world.WorldManager;

public class Server extends NetworkController {
	
	protected WorldManager worldUpdater;
	protected final SocketAddress[] clients;
	
	public Server(int maxClients) throws SocketException, UnknownHostException {
		super(new InetSocketAddress(InetAddress.getLocalHost(), 50000));
		clients = new SocketAddress[maxClients];
		messageHandler = new ServerMessageHandler();
	}
	
	protected class ServerMessageHandler implements MessageHandler {
		public void handle(ServerConnectRequest messsage, SocketAddress sender, int messageID) {
			boolean canConnect = clientsConnected() < clients.length && !clientPresent(sender);
			if(canConnect)
				addClient(sender);
			send(Protocol.construct(0, new ServerConnectAcknowledgment(canConnect), sender)); //TODO implement message ID, or is this always id 0?
		}

		@Override
		public void handle(ServerInfoRequest message, SocketAddress sender, int messageID) {
			System.out.println("received info request");
			send(Protocol.construct(0, new ServerInfo(clientsConnected(), clients.length), sender)); //TODO implement message ID, separate id for each client?
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
			new Thread(worldUpdater = new WorldManager(world), "Server World Updater").start();
		else
			throw new RuntimeException("A world is already running");
	}
	
	public void stopWorld() {
		if(worldUpdater != null && !worldUpdater.isFinished())
			Synchronizer.waitForExit(worldUpdater);
		else
			throw new RuntimeException("There is no world currently running");
	}
	
	public World getWorld() {
		return worldUpdater.getWorld();
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
