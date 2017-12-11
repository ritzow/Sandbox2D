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
import ritzow.sandbox.data.ByteUtil;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.data.SerializerReaderWriter;
import ritzow.sandbox.network.NetworkController;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.network.Protocol.PlayerAction;
import ritzow.sandbox.network.TimeoutException;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.entity.PlayerEntity;
import ritzow.sandbox.world.item.BlockItem;

public final class Server {
	private final NetworkController controller;
	private final ExecutorService broadcaster;
	private final Collection<ClientState> clients;
	private final ServerGameUpdater updater;
	private final SerializerReaderWriter serialRegistry;
	private volatile boolean canConnect;
	
	public Server(int port) throws SocketException, UnknownHostException {
		this(new InetSocketAddress(InetAddress.getLocalHost(), port));
	}
	
	public Server(InetSocketAddress bindAddress) throws SocketException {
		this.controller = new NetworkController(bindAddress, this::process);
		this.broadcaster = Executors.newCachedThreadPool();
		this.clients = Collections.synchronizedList(new LinkedList<ClientState>());
		this.serialRegistry = SerializationProvider.getProvider();
		this.updater = new ServerGameUpdater(this);
	}
	
	public void start(World world) {
		updater.start();
		updater.world = world;
		world.setRemoveEntities(e -> {
			sendRemoveEntity(e);
		});
		controller.start();
		canConnect = true;
	}
	
	public void stop() {
		canConnect = false;
		updater.stop();
		disconnectMultiple(clients, "Server shutting down");
		broadcaster.shutdown();
		controller.stop();
		try {
			broadcaster.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public ClientState[] listClients() {
		ClientState[] list = new ClientState[clients.size()];
		return clients.toArray(list);
	}
	
	private void process(InetSocketAddress sender, int messageID, byte[] data) {
		final short protocol = ByteUtil.getShort(data, 0);
		final ClientState client = forAddress(sender);
		try {
			if(client != null) {
				switch(protocol) {
					case Protocol.CLIENT_DISCONNECT:
						disconnect(client, false);
						break;
					case Protocol.CONSOLE_MESSAGE:
						processClientMessage(client, data);
						break;
					case Protocol.CLIENT_PLAYER_ACTION:
						processPlayerAction(client, data);
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
		} catch(ClientBadDataException e) {
			//disconnect the client if it sends invalid data three times or more
			if(client != null && client.strike() >= 3) {
				disconnect(client, "server received bad data from client 3 times");
			}
		}
	}
	
	private void processClientBreakBlock(ClientState client, byte[] data) {	
		int x = ByteUtil.getInteger(data, 2);
		int y = ByteUtil.getInteger(data, 6);
		checkBlockDestroyData(x, y);
		
		updater.submit(u -> {
			if(u.world != null) {
				Block block = u.world.getForeground().get(x, y);
				u.world.getForeground().destroy(u.world, x, y);
				sendRemoveBlock(block, x, y);
				//TODO block data needs to be reset on drop
				if(block != null) {
					ItemEntity<BlockItem> drop = 
							new ItemEntity<>(u.world.nextEntityID(), new BlockItem(block), x, y);
					drop.setVelocityX(-0.2f + ((float) Math.random() * (0.4f)));
					drop.setVelocityY((float) Math.random() * (0.35f));
					u.world.add(drop);
					sendEntity(drop);
				}
			}
		});
	}
	
	private void processClientMessage(ClientState client, byte[] data) {
		byte[] decoration = ('<' + client.username + "> ").getBytes(Protocol.CHARSET);
		byte[] broadcast = new byte[2 + decoration.length + data.length - 2];
		ByteUtil.putShort(broadcast, 0, Protocol.CONSOLE_MESSAGE);
		ByteUtil.copy(decoration, broadcast, 2);
		System.arraycopy(data, 2, broadcast, 2 + decoration.length, data.length - 2);
		broadcast(broadcast, true);
	}
	
	public void sendEntity(Entity e) {
		byte[] entity = serialRegistry.serialize(e);
		boolean compress = entity.length > Protocol.MAX_MESSAGE_LENGTH - 3;
		entity = compress ? ByteUtil.compress(entity) : entity;
		byte[] packet = new byte[3 + entity.length];
		ByteUtil.putShort(packet, 0, Protocol.SERVER_ADD_ENTITY);
		ByteUtil.putBoolean(packet, 2, compress); //always compressed
		ByteUtil.copy(entity, packet, 3);
		broadcast(packet);
	}
	
	public void sendEntityUpdate(Entity e) {
		//protocol, id, posX, posY, velX, velY
		byte[] update = new byte[2 + 4 + 4 + 4 + 4 + 4];
		ByteUtil.putShort(update, 0, Protocol.SERVER_ENTITY_UPDATE);
		ByteUtil.putInteger(update, 2, e.getID());
		ByteUtil.putFloat(update, 6, e.getPositionX());
		ByteUtil.putFloat(update, 10, e.getPositionY());
		ByteUtil.putFloat(update, 14, e.getVelocityX());
		ByteUtil.putFloat(update, 18, e.getVelocityY());
		broadcast(update);
	}
	
	public void sendRemoveBlock(Block block, int x, int y) {
		byte[] packet = new byte[10];
		ByteUtil.putShort(packet, 0, Protocol.SERVER_REMOVE_BLOCK);
		ByteUtil.putInteger(packet, 2, x);
		ByteUtil.putInteger(packet, 6, y);
		broadcast(packet);
	}
	
	private void sendPlayerIDMessage(PlayerEntity player, ClientState recipient) {
		byte[] packet = new byte[6];
		ByteUtil.putShort(packet, 0, Protocol.SERVER_PLAYER_ID);
		ByteUtil.putInteger(packet, 2, player.getID());
		send(packet, recipient);
	}
	
	public void sendRemoveEntity(Entity e) {
		byte[] packet = new byte[2 + 4];
		ByteUtil.putShort(packet, 0, Protocol.SERVER_REMOVE_ENTITY);
		ByteUtil.putInteger(packet, 2, e.getID());
		broadcast(packet);
	}
	
	public static byte[] buildServerDisconnect(String reason) {
		byte[] message = reason.getBytes(Protocol.CHARSET);
		byte[] packet = new byte[message.length + 6];
		ByteUtil.putShort(packet, 0, Protocol.SERVER_CLIENT_DISCONNECT);
		ByteUtil.putInteger(packet, 2, message.length);
		ByteUtil.copy(message, packet, 6);
		return packet;
	}
	
	private void checkBlockDestroyData(int x, int y) {
		int worldWidth = updater.world.getForeground().getWidth();
		int worldHeight = updater.world.getForeground().getHeight();
		if(updater.world == null || !(x < worldWidth && x >= 0 && y < worldHeight && y >= 0))
			throw new ClientBadDataException("client sent bad x and y block coordinates");
	}
	
	public static byte[][] buildWorldPackets(World world, Serializer ser) {
		Objects.requireNonNull(world);
		
		int headerSize = 2;
		int dataBytesPerPacket = Protocol.MAX_MESSAGE_LENGTH - headerSize;
	
		//serialize the world for transfer
		byte[] worldBytes = ByteUtil.compress(ser.serialize(world));
		
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
		synchronized(clients) { //synchronize entire method so clients cant connect mid-send
			if(clients.size() > 1) {
				CyclicBarrier barrier = new CyclicBarrier(clients.size() + 1); //all clients and calling thread
				for(ClientState client : clients) {
					//execute each reliable send on separate threads so they can run in parallel
					broadcaster.execute(() -> {
						try {
							controller.sendReliable(client.address, client.nextReliableID(), data, 10, 100);
							barrier.await(); //notify cyclic barrier that thread is done
						} catch(TimeoutException e) {
							if(removeUnresponsive) {
								try {
									//signal that the thread is finished so the disconnect can happen without blocking main
									barrier.await();
									disconnect(client, false);
								} catch (InterruptedException | BrokenBarrierException e1) {
									e1.printStackTrace();
								}
							}
						} catch (InterruptedException | BrokenBarrierException e) {
							e.printStackTrace();
						}
					});
				}
			
				try {
					barrier.await(); //wait until all clients have received the message
				} catch (InterruptedException | BrokenBarrierException e) {
					e.printStackTrace();
				}
			} else if(clients.size() == 1) { 
				//if there is only one client, skip synchronization
				ClientState client = clients.iterator().next();
				try {
					controller.sendReliable(client.address, client.nextReliableID(), data, 10, 100);
				} catch(TimeoutException e) {
					if(removeUnresponsive) {
						disconnect(client, false);
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
				controller.sendReliable(client.address, client.nextReliableID(), data, 10, 100);
			} catch(TimeoutException e) {
				if(removeUnresponsive) {
					disconnect(client, true);
				}
			}
		} else {
			controller.sendUnreliable(client.address, client.nextUnreliableID(), data);
		}
	}
	
	private static final byte[] PING_PACKET = new byte[2];
	
	static {
		ByteUtil.putShort(PING_PACKET, 0, Protocol.SERVER_PING);
	}
	
//	protected void pingClient(ClientState client, int unresponsiveMilliseconds) { //TODO should I get the actual ms ping each time I check to see if client is connected?
//		try {
//			//controller.sendUnreliable(client.address, client.nextUnreliableID(), PING_PACKET);
//			controller.sendReliable(client.address, client.nextReliableID(), PING_PACKET, 10, unresponsiveMilliseconds/10);
//		} catch(TimeoutException e) {
//			disconnect(client, true);
//		}
//	}
	
//	public void startWorld(ServerWorld world) {
//		if(worldUpdater == null || worldUpdater.isFinished()) {
//			world.setServer(this);
//			new Thread(worldUpdater = new WorldUpdater<ServerWorld>(world), "World Updater").start();
//		} else {
//			throw new RuntimeException("A world is already running");
//		}
//	}
//	
//	public World stopWorld() {
//		if(worldUpdater != null && worldUpdater.isRunning()) {
//			worldUpdater.waitForExit();
//			World world = worldUpdater.getWorld();
//			worldUpdater = null;
//			return world;
//		} else {
//			return null;
//		}
//	}
//	
//	public ServerWorld getWorld() {
//		return worldUpdater == null ? null : worldUpdater.getWorld();
//	}
	
	protected boolean isConnected(ClientState client) {
		return clients.contains(client);
	}
	
	/**
	 * Disconnects a client from the server
	 * @param client the client to disconnect
	 * @param notifyClient whether or not to notify the client that is being disconnected
	 */
	protected void disconnect(ClientState client, boolean notifyClient) {
		disconnect(client, notifyClient ? "" : null);
	}
	
	protected void disconnectMultiple(Collection<ClientState> clients, String reason) {
		synchronized(clients) {
			clients.parallelStream().forEach(client -> {
				if(reason != null)
					send(buildServerDisconnect(reason), client, false);
				controller.removeSender(client.address);
				if(client.player != null) {
					if(updater.isRunning()) {
						updater.submit(u -> {
							u.world.remove(client.player);
						});
					}
				}
			});
			clients.clear();
		}
	}
	
	/**
	 * Disconnects a client from the server
	 * @param client the client to disconnect
	 * @param reason the reason for disconnecting the client, or null to not notify the client
	 */
	protected void disconnect(ClientState client, String reason) {
		if(clients.remove(client)) {
			if(reason != null)
				send(buildServerDisconnect(reason), client, false);
			controller.removeSender(client.address);
			if(client.player != null) {
				updater.submit(u -> {
					u.world.remove(client.player);
					sendRemoveEntity(client.player);
				});
			}
			System.out.println(client + " disconnected (" + clients.size() + " players connected)");
		} else {
			System.out.println(client + " is not connected to the server");
		}
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
	
	private void sendClientConnectReply(SocketAddress client, int messageID, boolean connected) {
		byte[] response = new byte[3];
		ByteUtil.putShort(response, 0, Protocol.SERVER_CONNECT_ACKNOWLEDGMENT);
		ByteUtil.putBoolean(response, 2, canConnect);
		controller.sendReliable(client, messageID, response, 10, 100);
	}
	
	private void connectClient(InetSocketAddress client) {
		try {
			//determine if client can connect, and send a response
			boolean canConnect = this.canConnect;
			sendClientConnectReply(client, 0, canConnect);
			
			if(canConnect) {
				//create the client's ClientState object to track their information
				ClientState newClient = new ClientState(1, client);
				updater.submit(u -> {
					World world = u.world;
					
					//construct a new player for the client
					PlayerEntity player = new PlayerEntity(world.nextEntityID());
					newClient.player = player;
					
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
					
					world.add(player);
					sendEntity(player);
					
					//send the world to the client
					for(byte[] a : buildWorldPackets(world, serialRegistry)) {
						send(a, newClient);
					}
					
					clients.add(newClient);
					sendPlayerIDMessage(player, newClient);
					System.out.println(newClient + " joined (" + clients.size() + " players connected)");
				});
			}
		} catch(TimeoutException e) {
			System.out.println(client + " attempted to connect, but timed out");
		}
	}

	public final void processPlayerAction(ClientState client, byte[] data) {
		if(client.player == null)
			throw new ClientBadDataException("client has no associated player to perform an action");
		updater.submit(u -> {
			PlayerAction action = PlayerAction.forCode(data[2]);
			boolean enable = ByteUtil.getBoolean(data, 3);
			//TODO check if player can jump 
			client.player.processAction(action, enable);
		});
	}
	
	public int getConnectedClients() {
		return clients.size();
	}
	
	public static final class ClientState {
		private final InetSocketAddress address;
		private volatile int unreliableMessageID, reliableMessageID;
		private volatile String username;
		private volatile PlayerEntity player;
		private volatile short disconnectStrikes;
		
		public ClientState(int initReliable, InetSocketAddress address) {
			this.reliableMessageID = initReliable;
			this.address = address;
			username = "player_" + Integer.toString(Math.abs(address.hashCode()));
		}
		
		int nextUnreliableID() {
			return unreliableMessageID++;
		}
		
		int nextReliableID() {
			return reliableMessageID++;
		}
		
		int strike() {
			return ++disconnectStrikes;
		}
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof ClientState) {
				ClientState c = (ClientState)o;
				return unreliableMessageID == c.unreliableMessageID 
					&& reliableMessageID == c.reliableMessageID
					&& username.equals(c.username) 
					&& address.equals(c.address);
			}
			return false;
		}
		
		@Override
		public String toString() {
			return username + address.getAddress();
		}
	}
}
