package ritzow.sandbox.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
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
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.PlayerEntity;

public final class Server extends Runner {
	private static final long IDLE_CLIENT_DISCONNECT_MILLISECONDS = 3000;
	
	private final NetworkController controller;
	private final ExecutorService broadcaster;
	private final Collection<ClientState> clients;
	
	private volatile boolean canConnect;
	
	private final IdleClientDisconnector idleRemover; //can only be started once since it extends Thread
	private WorldUpdater<ServerWorld> worldUpdater;
	
	public Server(int port) throws SocketException, UnknownHostException {
		this(new InetSocketAddress(InetAddress.getLocalHost(), port));
	}
	
	public Server(SocketAddress bindAddress) throws SocketException, UnknownHostException {
		this.controller = new NetworkController(bindAddress);
		this.broadcaster = Executors.newCachedThreadPool();
		this.clients = Collections.synchronizedList(new LinkedList<ClientState>());
		this.idleRemover = new IdleClientDisconnector();
		controller.setOnRecieveMessage(this::process);
	}
	
	@Override
	protected void onStart() {
		controller.start();
		canConnect = true;
		idleRemover.start();
	}

	@Override
	protected void onStop() {
		try {
			canConnect = false;
			disconnectMultiple(clients, "Server shutting down");
			broadcaster.shutdown(); //stop the message broadcaster
			stopWorld();
			idleRemover.interrupt();
			idleRemover.waitForExit();
			controller.stop();
			broadcaster.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch(ServerWorldException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void process(SocketAddress sender, int messageID, byte[] data) {
		final short protocol = ByteUtil.getShort(data, 0);
		final ClientState client = forAddress(sender);
		
		if(client != null) {
			client.lastPing = System.nanoTime();
			switch(protocol) {
				case Protocol.CLIENT_DISCONNECT:
					disconnect(client, true);
					break;
				case Protocol.CONSOLE_MESSAGE:
					processClientMessage(client, data);
					break;
				case Protocol.CLIENT_PLAYER_ACTION:
					if(client.player != null)
						Protocol.PlayerAction.processPlayerAction(data, client.player);
					break;
				case Protocol.CLIENT_PING:
					client.lastPing = System.nanoTime();
					break;
				case Protocol.CLIENT_BREAK_BLOCK:
					processClientBreakBlock(client, data);
					break;
				default:
					System.out.println("received unknown protocol " + protocol);
			}
		} else if(protocol == Protocol.CLIENT_CONNECT_REQUEST) {
			connectClient(sender);
		}
	}
	
	private void processClientBreakBlock(ClientState client, byte[] data) {
		WorldUpdater<ServerWorld> wu = this.worldUpdater;
		if(wu != null && wu.isRunning()) {
			Objects.requireNonNull(client); Objects.requireNonNull(data);
			int x = ByteUtil.getInteger(data, 2);
			int y = ByteUtil.getInteger(data, 6);
			wu.getWorld().getForeground().destroy(wu.getWorld(), x, y); //TODO implement player distance checks
			broadcast(buildRemoveBlock(x, y));
		}
	}
	
	protected static byte[] buildRemoveBlock(int x, int y) {
		byte[] packet = new byte[10];
		ByteUtil.putShort(packet, 0, Protocol.SERVER_REMOVE_BLOCK);
		ByteUtil.putInteger(packet, 2, x);
		ByteUtil.putInteger(packet, 6, y);
		return packet;
	}

	protected void processClientMessage(ClientState client, byte[] data) {
		byte[] decoration = ('<' + client.username + "> ").getBytes(Protocol.CHARSET);
		byte[] broadcast = new byte[2 + decoration.length + data.length - 2];
		ByteUtil.putShort(broadcast, 0, Protocol.CONSOLE_MESSAGE);
		ByteUtil.copy(decoration, broadcast, 2);
		System.arraycopy(data, 2, broadcast, 2 + decoration.length, data.length - 2);
		broadcast(broadcast, true);
	}
	
	public void broadcastMessage(String message) {
		broadcast(Protocol.buildConsoleMessage(message));
	}
	
	public void broadcast(byte[] data) {
		broadcast(data, true);
	}
	
	/**
	 * Broadcasts {@code data} to each client connected to this Server and returns once all clients have received the data or
	 * become unresponsive, unless the data is unreliable, in which case the method will return immediately
	 * @param data the packet of data to send.
	 * @param removeUnresponsive whether or not to remove clients that do not respond from the server
	 */
	public void broadcast(byte[] data, boolean removeUnresponsive) {
		if(Protocol.isReliable(ByteUtil.getShort(data, 0))) {
			broadcastReliable(data, removeUnresponsive);
		} else {
			broadcastUnreliable(data);
		}
	}
	
	private void broadcastReliable(byte[] data, boolean removeUnresponsive) {
		synchronized(clients) {
			int clientCount = clientCount();
			if(clientCount > 1) {
				CyclicBarrier barrier = new CyclicBarrier(clientCount + 1); //all clients and calling thread
				synchronized(clients) {
					for(ClientState client : clients) {
						broadcaster.execute(() -> {
							try {
								controller.sendReliable(client.address, client.reliableMessageID++, data, 10, 100);
								barrier.await();
							} catch(TimeoutException e) {
								if(removeUnresponsive) {
									try {
										disconnect(client, false); //TODO could cause a problem?
										barrier.await();
									} catch (InterruptedException | BrokenBarrierException e1) {
										e1.printStackTrace();
									}
								}
							} catch (InterruptedException | BrokenBarrierException e) {
								e.printStackTrace();
							}
						});
					}
				}
				try {
					barrier.await(); //wait until all clients have received the message
				} catch (InterruptedException | BrokenBarrierException e) {
					e.printStackTrace();
				}
			} else if(clientCount == 1) { 
				//if there is only one client, there is no need to parallelify sending, reducing synchronization overhead
				for(ClientState client : clients) {
					try {
						controller.sendReliable(client.address, client.nextReliableID(), data, 10, 100);
					} catch(TimeoutException e) {
						if(removeUnresponsive) {
							disconnect(client, false); //TODO could cause a problem?
						}
					}
				}
			}
		}
	}
	
	private void broadcastUnreliable(byte[] data) {
		synchronized(clients) {
			for(ClientState client : clients) {
				controller.sendUnreliable(client.address, client.nextUnreliableID(), data);
			}
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
				if(removeUnresponsive) {
					disconnect(client, true);
				}
			}
		} else {
			controller.sendUnreliable(client.address, client.unreliableMessageID++, data);
		}
	}
	
	public void startWorld(ServerWorld world) {
		if(worldUpdater == null || worldUpdater.isFinished()) {
			world.setServer(this);
			new Thread(worldUpdater = new WorldUpdater<ServerWorld>(world), "World Updater").start();
		} else {
			throw new RuntimeException("A world is already running");
		}
	}
	
	public World stopWorld() {
		if(worldUpdater != null && worldUpdater.isRunning()) {
			worldUpdater.waitForExit();
			World world = worldUpdater.getWorld();
			worldUpdater = null;
			return world;
		} else {
			return null;
		}
	}
	
	public ServerWorld getWorld() {
		return worldUpdater == null ? null : worldUpdater.getWorld();
	}
	
	/**
	 * Disconnects a client from the server
	 * @param client the client to disconnect
	 * @param notifyClient whether or not to notify the client that is being disconnected
	 */
	protected void disconnect(ClientState client, boolean notifyClient) {
		disconnect(client, notifyClient ? "" : null);
	}
	
	/**
	 * Disconnects a client from the server
	 * @param client the client to disconnect
	 * @param reason the reason for disconnecting the client, or null to not notify the client
	 */
	protected void disconnect(ClientState client, String reason) {
		if(reason != null)
			send(Server.buildServerDisconnect(reason), client, false); //TODO implement reason thing
		controller.removeSender(client.address);
		if(client.player != null) {
			worldUpdater.getWorld().queueRemove(client.player);
		}
		clients.remove(client);
		System.out.println(client + " disconnected (" + clientCount() + " players connected)");
	}
	
	protected void disconnectMultiple(Collection<ClientState> clients, String reason) {
		clients.parallelStream().forEach(client -> { //TODO causes concurrent modification exception
			disconnect(client, reason);
		});
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
	
	private static byte[] createClientConnectReply(boolean canConnect) {
		byte[] response = new byte[3];
		ByteUtil.putShort(response, 0, Protocol.SERVER_CONNECT_ACKNOWLEDGMENT);
		ByteUtil.putBoolean(response, 2, canConnect);
		return response;
	}
	
	private void connectClient(SocketAddress client) {
		try {
			//determine if client can connect, and send a response
			boolean canConnect = this.canConnect;
			controller.sendReliable(client, 0, createClientConnectReply(canConnect), 10, 100);
			
			if(canConnect) {
				//create the client's ClientState object to track their information
				ClientState newClient = new ClientState(1, client);
				System.out.println(newClient + " connected");
				clients.add(newClient);
				
				//if a world is currently running
				if(worldUpdater != null && !worldUpdater.isFinished()) {
					ServerWorld world = worldUpdater.getWorld();
					
					//send the world to the client
					for(byte[] a : Server.buildWorldMessages(world)) {
						send(a, newClient);
					}
					
					//construct a new player for the client
					PlayerEntity player = new PlayerEntity(world.nextEntityID());
					
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
					
					//TODO could still be broken
					world.queueAdd(player);
					newClient.player = player;
					broadcast(buildSendEntity(player, true));
					send(createPlayerIDMessage(player), newClient);
				}
				newClient.lastPing = System.nanoTime();
			}
		} catch(TimeoutException e) {
			System.out.println(client + " attempted to connect, but timed out");
		}
	}

	public static byte[][] buildWorldMessages(ServerWorld world) {
		int headerSize = 2;
		int dataBytesPerPacket = Protocol.MAX_MESSAGE_LENGTH - headerSize;
	
		//serialize the world for transfer, no need to use ByteUtil.serialize because we know what we're serializing (standard world format)
		byte[] worldBytes = ByteUtil.compress(world.getBytes()); //serialize everything including other player entities
		
		//split world data into evenly sized packets and one extra packet if not evenly divisible by max packet size
		int packetCount = (worldBytes.length/dataBytesPerPacket) + (worldBytes.length % dataBytesPerPacket > 0 ? 1 : 0);
		
		//create the array to store all the constructed packets to send, in order
		byte[][] packets = new byte[1 + packetCount][];
	
		//create the first packet to send, which contains the number of subsequent packets
		byte[] head = new byte[6];
		ByteUtil.putShort(head, 0, Protocol.SERVER_WORLD_HEAD);
		ByteUtil.putInteger(head, 2, packetCount);
		packets[0] = head;
		
		//construct the packets containing the world data, which begin with a standard header and contain chunks of world bytes
		for(int slot = 1, index = 0; slot < packets.length; slot++) {
			int remaining = worldBytes.length - index;
			int dataSize = Math.min(dataBytesPerPacket, remaining);
			byte[] packet = new byte[headerSize + dataSize];
			ByteUtil.putShort(packet, 0, Protocol.SERVER_WORLD_DATA);
			System.arraycopy(worldBytes, index, packet, headerSize, dataSize);
			packets[slot] = packet;
			index += dataSize;
		}
		
		return packets;
	}

	public static byte[] buildServerDisconnect(String reason) {
		byte[] message = reason.getBytes(Protocol.CHARSET);
		byte[] packet = new byte[message.length + 6];
		ByteUtil.putShort(packet, 0, Protocol.SERVER_CLIENT_DISCONNECT);
		ByteUtil.putInteger(packet, 2, message.length);
		ByteUtil.copy(message, packet, 6);
		return packet;
	}

	protected static byte[] buildGenericEntityUpdate(Entity e) {
		//protocol, id, posX, posY, velX, velY
		byte[] update = new byte[2 + 4 + 4 + 4 + 4 + 4];
		ByteUtil.putShort(update, 0, Protocol.SERVER_ENTITY_UPDATE);
		ByteUtil.putInteger(update, 2, e.getID());
		ByteUtil.putFloat(update, 6, e.getPositionX());
		ByteUtil.putFloat(update, 10, e.getPositionY());
		ByteUtil.putFloat(update, 14, e.getVelocityX());
		ByteUtil.putFloat(update, 18, e.getVelocityY());
		return update;
	}

	public static byte[] buildRemoveEntity(Entity e) {
		byte[] packet = new byte[2 + 4];
		ByteUtil.putShort(packet, 0, Protocol.SERVER_REMOVE_ENTITY);
		ByteUtil.putInteger(packet, 2, e.getID());
		return packet;
	}

	protected static byte[] buildSendEntity(Entity e, boolean compressed) {
		byte[] serialized = compressed ? ByteUtil.compress(ByteUtil.serialize(e)) : ByteUtil.serialize(e);
		byte[] packet = new byte[2 + serialized.length];
		ByteUtil.putShort(packet, 0, compressed ? Protocol.SERVER_ADD_ENTITY_COMPRESSED : Protocol.SERVER_ADD_ENTITY);
		ByteUtil.copy(serialized, packet, 2);
		return packet;
	}
	
	private static byte[] createPlayerIDMessage(PlayerEntity player) {
		byte[] packet = new byte[6];
		ByteUtil.putShort(packet, 0, Protocol.SERVER_PLAYER_ID);
		ByteUtil.putInteger(packet, 2, player.getID());
		return packet;
	}
	
	/**
	 * Holder of information about clients connected to the server
	 */
	private static final class ClientState {
		private volatile int unreliableMessageID, reliableMessageID;
		protected final SocketAddress address;
		protected volatile String username;
		protected volatile PlayerEntity player;
		protected volatile long lastPing;
		
		public ClientState(int initReliable, SocketAddress address) {
			this.reliableMessageID = initReliable;
			this.address = address;
			username = "";
		}
		
		public int nextUnreliableID() {
			return unreliableMessageID++;
		}
		
		public int nextReliableID() {
			return reliableMessageID++;
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
	
	private final class IdleClientDisconnector extends Thread implements Service {
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
				try {
					synchronized(clients) {
						for(ClientState client : clients) {
							if(client != null && client.lastPing <= System.nanoTime()/1000000 - IDLE_CLIENT_DISCONNECT_MILLISECONDS) {
								disconnect(client, false);
							}
						}
					}
					
					Thread.sleep(IDLE_CLIENT_DISCONNECT_MILLISECONDS);
				} catch (InterruptedException e) {
					if(exit == true) {
						break;
					} else {
						e.printStackTrace();
					}
				}
			}
			
			synchronized(this) {
				finished = true;
				this.notifyAll();
			}
		}
	}
}
