package ritzow.sandbox.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import ritzow.sandbox.protocol.NetworkController;
import ritzow.sandbox.protocol.Protocol;
import ritzow.sandbox.protocol.TimeoutException;
import ritzow.sandbox.util.ByteUtil;
import ritzow.sandbox.util.Runner;
import ritzow.sandbox.util.Service;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.WorldUpdater;
import ritzow.sandbox.world.entity.PlayerEntity;

public final class Server extends Runner {
	private volatile int lastEntityID;
	private volatile boolean canConnect;
	private final NetworkController controller;
	private Service logicProcessor;
	private final ExecutorService broadcaster;
	private WorldUpdater worldUpdater;
	private final Collection<ClientState> clients;
	
	public Server(int port) throws SocketException, UnknownHostException {
		this.controller = new NetworkController(new InetSocketAddress(InetAddress.getLocalHost(), port));
		this.broadcaster = Executors.newCachedThreadPool();
		this.clients = Collections.synchronizedList(new LinkedList<ClientState>());
		this.canConnect = true;
		controller.setOnRecieveMessage(this::process);
	}
	
	@Override
	protected void onStart() {
		controller.start();
	}

	@Override
	protected void onStop() {
		try {
			this.canConnect = false;
			broadcaster.shutdown();
			if(controller.isRunning()) {
				broadcast(Protocol.buildServerDisconnect());
				disconnectAll();
				controller.waitForExit();
			}
			stopWorld();
			broadcaster.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			
			if(!controller.isRunning()) {
				synchronized(this) {
					notifyAll();
				}
			}
		} catch(ServerWorldException | InterruptedException e) {
			e.printStackTrace(); //ignores "no world to stop" exception
		} finally {
			controller.exit();
		}
	}
	
	protected final void process(SocketAddress sender, int messageID, byte[] data) {
		final short protocol = ByteUtil.getShort(data, 0);
		final ClientState client = forAddress(sender);
		
		if(client != null) {
			client.lastPing = System.nanoTime();
			switch(protocol) {
				case Protocol.CLIENT_DISCONNECT:
					disconnect(client);
					break;
				case Protocol.CONSOLE_MESSAGE:
					processClientMessage(client, data);
					break;
				case Protocol.CLIENT_PLAYER_ACTION:
					if(client.player != null)
						Protocol.PlayerAction.processPlayerMovementAction(data, client.player);
					break;
				case Protocol.CLIENT_PING:
					break;
				default:
					System.out.println("received unknown protocol " + protocol);
			}
		} else if(protocol == Protocol.CLIENT_CONNECT_REQUEST) {
			connectClient(sender);
		}
	}
	
	protected void processClientMessage(ClientState client, byte[] data) {
		byte[] decoration = ('<' + client.username + "> ").getBytes(Protocol.CHARSET);
		byte[] broadcast = new byte[2 + decoration.length + data.length - 2];
		ByteUtil.putShort(broadcast, 0, Protocol.CONSOLE_MESSAGE);
		ByteUtil.copy(decoration, broadcast, 2);
		System.arraycopy(data, 2, broadcast, 2 + decoration.length, data.length - 2);
		broadcast(broadcast);
	}
	
	public void broadcastMessage(String message) {
		broadcast(Protocol.buildConsoleMessage(message));
	}
	
	public void broadcast(byte[] data) {
		broadcast(data, true);
	}
	
	/**
	 * Broadcasts {@code data} to each client connected to this Server and returns immediately
	 * @param data the packet of data to send
	 */
	public void broadcast(byte[] data, boolean removeUnresponsive) {
		boolean reliable = Protocol.isReliable(ByteUtil.getShort(data, 0));
		synchronized(clients) {
			clients.forEach(client -> {
				if(client != null) {
					if(reliable) {
						broadcaster.execute(() -> {
							try {
								controller.sendReliable(client.address, client.reliableMessageID++, data, 10, 100);
							} catch(TimeoutException e) {
								if(removeUnresponsive) {
									remove(client); //TODO this could cause a concurrent modification because of clients.forEach using a foreach loop
								}
							}
						});
					} else {
						controller.sendUnreliable(client.address, client.unreliableMessageID++, data);
					}
				}
			});
		}
	}
	
	protected void send(byte[] data, ClientState client) {
		this.send(data, client, true);
	}
	
	protected void send(byte[] data, ClientState client, boolean removeUnresponsive) {
		if(Protocol.isReliable(ByteUtil.getShort(data, 0))) {
			try {
				controller.sendReliable(client.address, client.reliableMessageID++, data, 10, 100);
			} catch(TimeoutException e) {
				if(removeUnresponsive && clients.contains(client)) {
					disconnect(client); //TODO improve unresponsive client disconnecting
				}
			}
		} else {
			controller.sendUnreliable(client.address, client.unreliableMessageID++, data);
		}
	}
	
	public void startWorld(World world) {
		if(worldUpdater == null || worldUpdater.isFinished()) {
			new Thread(worldUpdater = new WorldUpdater(world), "Server World Updater").start();
			new Thread(logicProcessor = new IdleClientDisconnector(), "World Logic Updater").start();
		} else {
			throw new RuntimeException("A world is already running");
		}
	}
	
	/**
	 * 
	 * @throws ServerWorldException if there is no world running
	 */
	public void stopWorld() throws ServerWorldException {
		if(worldUpdater != null && worldUpdater.isRunning()) {
			worldUpdater.exit();
			if(logicProcessor != null && logicProcessor.isRunning()) {
				logicProcessor.exit();
			}
		} else {
			throw new ServerWorldException("There is no world currently running");
		}
	}
	
	public World getWorld() {
		return worldUpdater == null ? null : worldUpdater.getWorld();
	}
	
	/** Disconnects a client without telling the client **/
	protected void remove(ClientState client) {
		controller.removeSender(client.address);
		if(client.player != null) {
			worldUpdater.getWorld().remove(client.player);
		}
		clients.remove(client);
		System.out.println(client + " disconnected (" + clientCount() + " players connected)");
	}
	
	/** Disconnects all clients without telling any of them **/
	protected void removeAll() {
		controller.removeSenders();
		clients.forEach(client -> {
			if(client.player != null) {
				worldUpdater.getWorld().remove(client.player);
			}
		});
		clients.clear();
	}
	
	/** Disconnects a client after notifying it **/
	protected void disconnect(ClientState client) {
		send(Protocol.buildServerDisconnect(), client, false);
		remove(client);
	}
	
	/** Disconnect all clients after notifying them **/
	protected void disconnectAll() {
		broadcast(Protocol.buildServerDisconnect(), false);
		removeAll();
	}
	
	/**
	 * Finds and returns the {@link ClientState} associated with address
	 * @param address the socket address of the desired client
	 * @return the client associated with SocketAddress {@code address}
	 */
	protected ClientState forAddress(SocketAddress address) {
		synchronized(clients) {
			for(ClientState client : clients) {
				if(client.address.equals(address)) {
					return client;
				}
			}
		}
		return null;
	}
	
	/**
	 * @return the number of currently connected clients
	 */
	public int clientCount() {
		int count = 0;
		synchronized(clients) {
			for(ClientState client : clients) {
				if(client != null) {
					count++;
				}
			}
		}
		return count;
	}
	
	private void connectClient(SocketAddress client) {
		//determine if client can connect, and send a response
		boolean canConnect = this.canConnect;
		byte[] response = new byte[3];
		ByteUtil.putShort(response, 0, Protocol.SERVER_CONNECT_ACKNOWLEDGMENT);
		ByteUtil.putBoolean(response, 2, canConnect);
		controller.sendReliable(client, 0, response, 10, 100);
		
		if(canConnect) {
			//create the client's ClientState object to track their information
			ClientState newClient = new ClientState(1, client);
			System.out.println(newClient + " connected");
			
			//if a world is currently running
			if(worldUpdater != null && !worldUpdater.isFinished()) {
				
				//send the world to the client
				for(byte[] a : Protocol.constructWorldPackets(worldUpdater.getWorld())) {
					send(a, newClient);
				}
				
				//construct a new player for the client
				PlayerEntity player = new PlayerEntity(++lastEntityID);
				newClient.player = player;
				
				World world = worldUpdater.getWorld();
				
				//set the player's position to directly above the ground in the center of the world
				player.setPositionX(world.getForeground().getWidth()/2);
				for(int i = world.getForeground().getHeight() - 2; i > 1; i--) {
					if(world.getForeground().get(player.getPositionX(), i) == null 
							&& world.getForeground().get(player.getPositionX(), i + 1) == null 
							&& world.getForeground().get(player.getPositionX(), i - 1) != null) {
						player.setPositionY(i);
						break;
					}
				}
				
				//send the player object to the client
				byte[] playerData = ByteUtil.compress(ByteUtil.serialize(player));
				byte[] playerPacket = new byte[2 + playerData.length];
				ByteUtil.putShort(playerPacket, 0, Protocol.SERVER_PLAYER_ENTITY_COMPRESSED);
				ByteUtil.copy(playerData, playerPacket, 2);
				send(playerPacket, newClient);
				
				//change the packet to an add entity packet and broadcast it to all already-connected clients
				ByteUtil.putShort(playerPacket, 0, Protocol.SERVER_ADD_ENTITY_COMPRESSED);
				broadcast(playerPacket);
				world.add(player);
				newClient.lastPing = System.nanoTime();
			}
			
			//add the newly connected client to the clients list
			clients.add(newClient);
		}
	}
	
	/**
	 * Holder of information about clients connected to the server
	 */
	private static final class ClientState {
		protected final SocketAddress address;
		protected volatile String username;
		protected volatile PlayerEntity player;
		protected volatile int unreliableMessageID, reliableMessageID;
		protected volatile long lastPing;
		
		public ClientState(int initReliable, SocketAddress address) {
			this.reliableMessageID = initReliable;
			this.address = address;
			username = "Unknown";
		}
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof ClientState) {
				ClientState c = (ClientState)o;
				return 
					username.equals(c.username) 
					&& address.equals(c.address) 
					&& unreliableMessageID == c.unreliableMessageID 
					&& reliableMessageID == c.reliableMessageID;
			} else {
				return false;
			}
		}
		
		@Override
		public String toString() {
			return username + " " + address;
		}
	}
	
	private final class IdleClientDisconnector implements Service { //TODO build into world updating
		private boolean setup, exit, finished;
		
		@Override
		public synchronized boolean isSetupComplete() {
			return setup;
		}

		@Override
		public synchronized void exit() {
			exit = true;
			this.notifyAll();
		}

		@Override
		public synchronized boolean isFinished() {
			return finished;
		}

		@Override
		public void run() {
			synchronized(this) {
				setup = true;
				this.notifyAll();
			}
			
			while(!exit) {
				for(ClientState client : clients) {
					if(client != null && client.lastPing <= System.nanoTime() - 3000000000L) { //if lastPing was greater than 3 seconds ago
						disconnect(client); //TODO will cause concurrent mod
					}
				}
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			synchronized(this) {
				finished = true;
				this.notifyAll();
			}
		}
	}
}
