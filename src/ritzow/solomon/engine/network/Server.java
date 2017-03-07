package ritzow.solomon.engine.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.util.Service;
import ritzow.solomon.engine.world.base.World;
import ritzow.solomon.engine.world.base.WorldUpdater;
import ritzow.solomon.engine.world.entity.Entity;
import ritzow.solomon.engine.world.entity.PlayerEntity;

public final class Server extends NetworkController {
	protected Service logicProcessor;
	protected WorldUpdater worldUpdater;
	protected volatile int lastEntityID;
	protected final List<ClientState> clients;
	
	protected final ExecutorService broadcaster;
	
	public Server() throws SocketException, UnknownHostException {
		this(20);
	}
	
	public Server(int maxClients) throws SocketException, UnknownHostException {
		super(new InetSocketAddress(InetAddress.getLocalHost(), 50000));
		this.broadcaster = Executors.newCachedThreadPool();
		this.clients = Collections.synchronizedList(new LinkedList<ClientState>());
	}
	
	public void broadcast(byte[] data) {
		boolean reliable = Protocol.isReliable(ByteUtil.getShort(data, 0));
		
		synchronized(clients) {
			for(ClientState client : clients) {
				if(client != null) {
					if(reliable) {
						broadcaster.execute(() -> {
							try {
								super.sendReliable(client.address, client.reliableMessageID++, data, 10, 100);
							} catch(TimeoutException e) {
								if(clients.contains(client)) {
									removeAndDisconnect(client);
								}
							}
						});
					} else {
						sendUnreliable(client.address, client.unreliableMessageID++, data);
					}
				}
			}
		}
	}
	
	protected void send(byte[] data, ClientState client) {
		if(Protocol.isReliable(ByteUtil.getShort(data, 0))) {
			try {
				super.sendReliable(client.address, client.reliableMessageID++, data, 10, 100);
			} catch(TimeoutException e) {
				if(clients.contains(client)) {
					removeAndDisconnect(client);
				}
			}
		} else {
			super.sendUnreliable(client.address, client.unreliableMessageID++, data);
		}
	}
	
	@Override
	protected final void process(SocketAddress sender, int messageID, byte[] data) {
		final short protocol = ByteUtil.getShort(data, 0);
		final ClientState client = forAddress(sender);
		
		if(client != null) {
			switch(protocol) {
			case Protocol.CLIENT_DISCONNECT:
				removeAndDisconnect(client);
				break;
			case Protocol.CONSOLE_MESSAGE:
				byte[] decoration = ('<' + client.username + "> ").getBytes();
				byte[] broadcast = new byte[2 + decoration.length + data.length - 2];
				ByteUtil.putShort(broadcast, 0, Protocol.CONSOLE_MESSAGE);
				ByteUtil.copy(decoration, broadcast, 2);
				System.arraycopy(data, 2, broadcast, 2 + decoration.length, data.length - 2);
				broadcast(broadcast);
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
			//determine if client can connect, and send a response
			boolean canConnect = true; //clientCount() < clients.size();
			byte[] response = new byte[3];
			ByteUtil.putShort(response, 0, Protocol.SERVER_CONNECT_ACKNOWLEDGMENT);
			ByteUtil.putBoolean(response, 2, canConnect);
			super.sendReliable(sender, 0, response, 10, 100);
			
			if(canConnect) {
				//create the client's ClientState object to track their information
				ClientState newClient = new ClientState(1, sender);
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
	}
	
	@Override
	public void exit() {
		try {
			broadcast(Protocol.buildServerDisconnect());
			stopWorld();
			synchronized(clients) {
				Iterator<ClientState> iterator = clients.iterator();
				while(iterator.hasNext()) {
					ClientState client = iterator.next();
					if(client != null) {
						disconnect(client);
					}
				}
				clients.clear();
			}
		} catch(RuntimeException e) {
			//ignores "no world to stop" exception
		} finally {
			super.exit();
		}
	}
	
	public void startWorld(World world) {
		if(worldUpdater == null || worldUpdater.isFinished()) {
			new Thread(worldUpdater = new WorldUpdater(world), "Server World Updater").start();
			new Thread(logicProcessor = new WorldLogicProcessor(world), "World Logic Updater").start();
		} else {
			throw new RuntimeException("A world is already running");	
		}
	}
	
	public void stopWorld() {
		if(worldUpdater != null && worldUpdater.isRunning()) {
			worldUpdater.exit();
			if(logicProcessor != null && logicProcessor.isRunning())
			logicProcessor.exit();
		} else {
			throw new RuntimeException("There is no world currently running");
		}
	}
	
	public World getWorld() {
		return worldUpdater == null ? null : worldUpdater.getWorld();
	}
	
	/**
	 * Removes ClientState {@code client} from {@code clients} and disconnects it.
	 * @param client the client to remove and disconnect
	 */
	protected void removeAndDisconnect(ClientState client) {
		clients.remove(client);
		disconnect(client);
	}
	
	/**
	 * Disconnects ClientState {@code client}, but does not remove it from the list of clients.
	 * @param client the client to disconnect
	 */
	protected void disconnect(ClientState client) {
		System.out.println(client + " disconnected");
		removeSender(client.address);
		if(worldUpdater != null)
			worldUpdater.getWorld().remove(client.player);
		broadcast(Protocol.buildGenericEntityRemoval(client.player.getID()));
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
			username = "anonymous";
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
							broadcast(Protocol.buildGenericEntityUpdate(e));
						}
					}
					Thread.sleep(100);
				} catch (Exception e) {
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
