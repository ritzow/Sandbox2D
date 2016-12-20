package ritzow.solomon.engine.network.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import ritzow.solomon.engine.network.NetworkController;
import ritzow.solomon.engine.network.message.MessageHandler;
import ritzow.solomon.engine.network.message.RequestDenial;
import ritzow.solomon.engine.network.message.client.ServerConnectRequest;
import ritzow.solomon.engine.network.message.client.ServerInfoRequest;
import ritzow.solomon.engine.network.message.client.WorldRequest;
import ritzow.solomon.engine.network.message.server.ServerConnectAcknowledgment;
import ritzow.solomon.engine.network.message.server.ServerInfo;
import ritzow.solomon.engine.network.message.server.world.WorldCreationMessage;
import ritzow.solomon.engine.network.message.server.world.WorldDataMessage;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.world.World;
import ritzow.solomon.engine.world.WorldManager;

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
			//send(Protocol.construct(0, new ServerConnectAcknowledgment(canConnect), sender)); //TODO implement message ID, or is this always id 0?
			send(new ServerConnectAcknowledgment(canConnect), sender);
		}

		@Override
		public void handle(ServerInfoRequest message, SocketAddress sender, int messageID) {
			System.out.println("received info request");
			//send(Protocol.construct(0, new ServerInfo(clientsConnected(), clients.length), sender)); //TODO implement message ID, separate id for each client?
			send(new ServerInfo(clientsConnected(), clients.length), sender);
		}

		@Override
		public void handle(WorldRequest message, SocketAddress sender, int messageID) {
			boolean connected = clientPresent(sender);
			boolean isWorld = worldUpdater != null;
			boolean isRunning = isWorld && !worldUpdater.isFinished();
			if(connected && isWorld && isRunning) {
				byte[] world = ByteUtil.serialize(getWorld());
				int index = 0;
				Server.this.send(new WorldCreationMessage(world.length), sender);
				Server.this.send(new WorldDataMessage(Arrays.copyOfRange(world, index, Math.min(index + 500, world.length))), sender);
			} else {
				String reason;
				if(!connected)
					reason = "not connected to server";
				else if(!isWorld)
					reason = "no world to send";
				else if(isWorld && !isRunning)
					reason = "world not currently running";
				else
					reason = "an error occurred";
				send(new RequestDenial(reason), sender);
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
			new Thread(worldUpdater = new WorldManager(world), "Server World Updater").start();
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
