package ritzow.solomon.engine.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.util.Service;
import ritzow.solomon.engine.world.base.ClientWorldUpdater;
import ritzow.solomon.engine.world.base.World;
import ritzow.solomon.engine.world.entity.Entity;
import ritzow.solomon.engine.world.entity.PlayerEntity;

public final class Server extends NetworkController {
	private volatile int lastEntityID;
	private volatile boolean canConnect;
	
	private Service logicProcessor;
	private final ExecutorService broadcaster;
	private ClientWorldUpdater worldUpdater;
	private final List<ClientState> clients;
	
	public Server() throws SocketException, UnknownHostException {
		this(20);
	}
	
	public Server(int maxClients) throws SocketException, UnknownHostException {
		super(new InetSocketAddress(InetAddress.getLocalHost(), 50000));
		this.broadcaster = Executors.newCachedThreadPool();
		this.clients = Collections.synchronizedList(new LinkedList<ClientState>());
		this.canConnect = true;
	}
	
	@Override
	protected final void process(SocketAddress sender, int messageID, byte[] data) {
		final short protocol = ByteUtil.getShort(data, 0);
		final ClientState client = forAddress(sender);
		
		if(client != null) {
			switch(protocol) {
				case Protocol.CLIENT_DISCONNECT:
					remove(client);
					break;
				case Protocol.CONSOLE_MESSAGE:
					processClientMessage(client, data);
					break;
				case Protocol.CLIENT_PLAYER_ACTION:
					if(client.player != null) {
						Protocol.PlayerAction.processPlayerMovementAction(data, client.player);
					}
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
		this.broadcast(Protocol.buildConsoleMessage(message));
	}
	
	/**
	 * Broadcasts {@code data} to each client connected to this Server and returns immediately
	 * @param data the packet of data to send
	 */
	public void broadcast(byte[] data) {
		boolean reliable = Protocol.isReliable(ByteUtil.getShort(data, 0));
		synchronized(clients) {
			clients.forEach(client -> {
				if(client != null) {
					if(reliable) {
						broadcaster.execute(() -> {
							try {
								super.sendReliable(client.address, client.reliableMessageID++, data, 10, 100);
							} catch(TimeoutException e) {
								remove(client);
							}
						});
					} else {
						sendUnreliable(client.address, client.unreliableMessageID++, data);
					}
				}
			});
		}
	}
	
	protected void send(byte[] data, ClientState client) {
		if(Protocol.isReliable(ByteUtil.getShort(data, 0))) {
			try {
				super.sendReliable(client.address, client.reliableMessageID++, data, 10, 100);
			} catch(TimeoutException e) {
				if(clients.contains(client)) {
					remove(client);
				}
			}
		} else {
			super.sendUnreliable(client.address, client.unreliableMessageID++, data);
		}
	}
	
	public void startWorld(World world) {
		if(worldUpdater == null || worldUpdater.isFinished()) {
			new Thread(worldUpdater = new ClientWorldUpdater(world), "Server World Updater").start();
			new Thread(logicProcessor = new WorldLogicProcessor(world), "World Logic Updater").start();
		} else {
			throw new RuntimeException("A world is already running");
		}
	}
	
	public void stopWorld() {
		if(worldUpdater != null && worldUpdater.isRunning()) {
			worldUpdater.exit();
			if(logicProcessor != null && logicProcessor.isRunning()) {
				logicProcessor.exit();
			}
		} else {
			throw new RuntimeException("There is no world currently running");
		}
	}
	
	public World getWorld() {
		return worldUpdater == null ? null : worldUpdater.getWorld();
	}
	
	/** Disconnects a client without telling the client **/
	protected void remove(ClientState client) {
		super.removeSender(client.address);
		if(client.player != null) {
			worldUpdater.getWorld().remove(client.player);
		}
		clients.remove(client);
		System.out.println(client + " disconnected");
	}
	
	/** Disconnects all clients without telling any of them **/
	protected void removeAll() {
		super.removeSenders();
		clients.forEach(client -> {
			if(client.player != null) {
				worldUpdater.getWorld().remove(client.player);
			}
		});
		clients.clear();
		System.out.println("Disconnected all clients");
	}
	
	/** Disconnects a client after notifying it **/
	protected void disconnect(ClientState client) {
		send(Protocol.buildServerDisconnect(), client);
		remove(client);
	}
	
	/** Disconnect all clients after notifying them **/
	protected void disconnectAll() {
		broadcast(Protocol.buildServerDisconnect());
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
	protected int clientCount() {
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
		super.sendReliable(client, 0, response, 10, 100);
		
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
			}
			
			//add the newly connected client to the clients list
			clients.add(newClient);
		}
	}
	
	@Override
	public void exit() {
		try {
			this.canConnect = false;
			broadcast(Protocol.buildServerDisconnect());
			stopWorld();
			logicProcessor.exit();
			disconnectAll();
			broadcaster.shutdown();
			broadcaster.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			logicProcessor.waitUntilFinished();
		} catch(RuntimeException | InterruptedException e) {
			//ignores "no world to stop" exception
		} finally {
			super.exit();
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
	
	private final class WorldLogicProcessor implements Service {
		private boolean setup, exit, finished;
		private final World world;
		
		public WorldLogicProcessor(World world) {
			this.world = world;
		}

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
				try {
					for(Entity e : world) {
						synchronized(e) {
							//TODO build this into some sort of ServerWorldUpdater, perhaps put all message sending in one thread?
							broadcast(Protocol.buildGenericEntityUpdate(e));
						}
					}
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}
			}
			
			synchronized(this) {
				finished = true;
				this.notifyAll();
			}
		}
	}
}
