package ritzow.sandbox.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import ritzow.sandbox.data.ByteUtil;
import ritzow.sandbox.network.NetworkController;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.network.Protocol.PlayerAction;
import ritzow.sandbox.network.TimeoutException;
import ritzow.sandbox.server.world.entity.ServerBombEntity;
import ritzow.sandbox.util.SharedConstants;
import ritzow.sandbox.util.TaskQueue;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.entity.BombEntity;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.entity.PlayerEntity;
import ritzow.sandbox.world.item.BlockItem;

public class Server {
	private final NetworkController network;
	private final ExecutorService receivePool, broadcastPool;
	private final Map<InetSocketAddress, ClientState> clients;
	private volatile boolean canConnect;
	private long lastWorldUpdateTime, lastEntitySendTime;
	private World world;
	private final TaskQueue processQueue;
	
	private static final long NETWORK_SEND_INTERVAL_NANOSECONDS = Utility.millisToNanos(150);
	
	public static Server open(InetSocketAddress bindAddress) throws IOException {
		return new Server(bindAddress);
	}
	
	private Server(InetSocketAddress bindAddress) throws IOException {
		this.network = new NetworkController(bindAddress, this::receive);
		ThreadGroup processors = new ThreadGroup("Message Processors");
		ThreadGroup senders = new ThreadGroup("Broadcaster Group");
		this.receivePool = Executors.newFixedThreadPool(10,
				runnable -> new Thread(processors, runnable, "Processor"));
		AtomicInteger id = new AtomicInteger();
		this.broadcastPool = Executors.newCachedThreadPool(
				runnable -> new Thread(senders, runnable, "Broadcaster " + id.incrementAndGet()));
		this.clients = new ConcurrentHashMap<>();
		this.processQueue = new TaskQueue();
	}
	
	public ClientState[] listClients() {
		return clients.values().toArray(new ClientState[clients.size()]);
	}
	
	public void start(World world) {
		this.world = world;
		world.setRemoveEntities(this::sendRemoveEntity);
		lastWorldUpdateTime = System.nanoTime();
		lastEntitySendTime = lastWorldUpdateTime;
		network.start();
		canConnect = true;
	}
	
	public void stop() {
		canConnect = false;
		disconnectAll("server shutting down");
		receivePool.shutdown();
		broadcastPool.shutdown();
		network.stop();
		try {
			receivePool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			broadcastPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void receive(InetSocketAddress sender, byte[] data) {
		short type = ByteUtil.getShort(data, 0);
		ClientState client = clients.get(sender);

		if(client == null && type == Protocol.CLIENT_CONNECT_REQUEST) {
			receivePool.execute(() -> connectClient(sender));
		} else if(client != null && type != Protocol.CLIENT_CONNECT_REQUEST) {
			receivePool.execute(() -> process(client, type, data));
		}
	}
	
	private void process(ClientState client, short type, byte[] data) {
		try {
			switch(type) {
				case Protocol.CLIENT_DISCONNECT:
					removeClient(client);
					broadcastAndPrint(getClientDisconnectMessage(client));
					break;
				case Protocol.CLIENT_PLAYER_ACTION:
					processPlayerAction(client, data);
					break;
				case Protocol.CLIENT_BREAK_BLOCK:
					processClientBreakBlock(client, data);
					break;
				case Protocol.CLIENT_BOMB_THROW:
					processClientThrowBomb(client, data);
					break;
				case Protocol.CLIENT_WORLD_BUILT:
					client.worldSetupComplete = true;
					break;
				default:
					throw new ClientBadDataException("received unknown protocol " + type);
			}
		} catch(ClientBadDataException e) {
			if(client.strike() == 3) {
				//disconnect the client if it sends invalid data three times or more
				disconnect(client, "Sent bad data three times");
			}
		}
	}
	
	private static final float BOMB_THROW_VELOCITY = 0.8f;
	
	private void processClientThrowBomb(ClientState client, byte[] data) {
		processQueue.add(() -> {
			BombEntity bomb = new ServerBombEntity(this, world.nextEntityID());
			bomb.setPositionX(client.player.getPositionX());
			bomb.setPositionY(client.player.getPositionY());
			float angle = ByteUtil.getFloat(data, 2);
			bomb.setVelocityX((float) (Math.cos(angle) * BOMB_THROW_VELOCITY));
			bomb.setVelocityY((float) (Math.sin(angle) * BOMB_THROW_VELOCITY));
			world.add(bomb);
			sendAddEntity(bomb);
		});
	}
	
	private final byte[] update = new byte[2 + 4 + 4 + 4 + 4 + 4]; //protocol, id, posX, posY, velX, velY
	
	{
		ByteUtil.putShort(update, 0, Protocol.SERVER_ENTITY_UPDATE);
	}

	public void update() {
		processQueue.run();
		lastWorldUpdateTime = Utility.updateWorld(world, lastWorldUpdateTime, 
				SharedConstants.MAX_TIMESTEP, SharedConstants.TIME_SCALE_NANOSECONDS);
		if(Utility.nanosSince(lastEntitySendTime) > NETWORK_SEND_INTERVAL_NANOSECONDS) {
			byte[] update = this.update;
			for(Entity e : world) {
				ByteUtil.putInteger(update, 2, e.getID());
				ByteUtil.putFloat(update, 6, e.getPositionX());
				ByteUtil.putFloat(update, 10, e.getPositionY());
				ByteUtil.putFloat(update, 14, e.getVelocityX());
				ByteUtil.putFloat(update, 18, e.getVelocityY());
				broadcastUnreliable(update, client -> client.worldSetupComplete);
			}
			pingClients(); //send a reliable packet to make sure clients are connected
			lastEntitySendTime = System.nanoTime();
		}
	}
	
	private void processPlayerAction(ClientState client, byte[] data) {
		if(client.player == null)
			throw new ClientBadDataException("client has no associated player to perform an action");
		processQueue.add(()->{
			PlayerAction action = PlayerAction.forCode(data[2]);
			boolean enable = ByteUtil.getBoolean(data, 3);
			client.player.processAction(action, enable);
			broadcastPlayerAction(client.player, action, enable);
		});
	}
	
	private void broadcastPlayerAction(PlayerEntity player, PlayerAction action, boolean isEnabled) {
		byte[] packet = new byte[8];
		ByteUtil.putShort(packet, 0, Protocol.SERVER_PLAYER_ACTION);
		ByteUtil.putInteger(packet, 2, player.getID());
		packet[6] = action.getCode();
		ByteUtil.putBoolean(packet, 7, isEnabled);
		broadcastReliable(packet, true);
	}
	
	private void processClientBreakBlock(ClientState client, byte[] data) {
		int x = ByteUtil.getInteger(data, 2);
		int y = ByteUtil.getInteger(data, 6);
		if(!world.getForeground().isValid(x, y))
			throw new ClientBadDataException("client sent bad x and y block coordinates");
		processQueue.add(()->{
			Block block = world.getForeground().get(x, y);
			if(world.getForeground().destroy(world, x, y)) {
				//TODO block data needs to be reset on drop
				var drop = new ItemEntity<BlockItem>(world.nextEntityID(), new BlockItem(block), x, y);
				drop.setVelocityX(-0.2f + ((float) Math.random() * (0.4f)));
				drop.setVelocityY((float) Math.random() * (0.35f));
				world.add(drop);
				sendRemoveBlock(x, y);
				sendAddEntity(drop);
			}
		});
	}
	
	private static final byte[] PING;
	
	static {
		PING = new byte[2];
		ByteUtil.putShort(PING, 0, Protocol.PING);
	}
	
	public void pingClients() {
		broadcastReliableAsync(PING, true, client -> client.worldSetupComplete);
	}
	
	public void broadcastConsoleMessage(String message) {
		broadcastReliable(Protocol.buildConsoleMessage(message), true);
	}
	
	public void sendRemoveBlock(int x, int y) {
		byte[] packet = new byte[10];
		ByteUtil.putShort(packet, 0, Protocol.SERVER_REMOVE_BLOCK);
		ByteUtil.putInteger(packet, 2, x);
		ByteUtil.putInteger(packet, 6, y);
		broadcastReliable(packet, true);
	}
	
	private void sendPlayerID(PlayerEntity player, ClientState recipient) {
		byte[] packet = new byte[6];
		ByteUtil.putShort(packet, 0, Protocol.SERVER_PLAYER_ID);
		ByteUtil.putInteger(packet, 2, player.getID());
		sendReliable(recipient, packet, true);
	}
	
	public void sendAddEntity(Entity e) {
		byte[] entity = SerializationProvider.getProvider().serialize(e);
		boolean compress = entity.length > Protocol.MAX_MESSAGE_LENGTH - 3;
		entity = compress ? ByteUtil.compress(entity) : entity;
		byte[] packet = new byte[3 + entity.length];
		ByteUtil.putShort(packet, 0, Protocol.SERVER_ADD_ENTITY);
		ByteUtil.putBoolean(packet, 2, compress);
		ByteUtil.copy(entity, packet, 3);
		broadcastReliable(packet, true);
	}
	
	public void sendRemoveEntity(Entity e) {
		byte[] packet = new byte[2 + 4];
		ByteUtil.putShort(packet, 0, Protocol.SERVER_REMOVE_ENTITY);
		ByteUtil.putInteger(packet, 2, e.getID());
		broadcastReliable(packet, true);
	}
	
	private static byte[] buildServerDisconnect(String reason) {
		byte[] message = reason.getBytes(Protocol.CHARSET);
		byte[] packet = new byte[message.length + 6];
		ByteUtil.putShort(packet, 0, Protocol.SERVER_CLIENT_DISCONNECT);
		ByteUtil.putInteger(packet, 2, message.length);
		ByteUtil.copy(message, packet, 6);
		return packet;
	}
	
	public boolean isConnected(InetSocketAddress address) {
		return clients.containsKey(address);
	}
	
	public int getConnectedClients() {
		return clients.size();
	}
	
	/**
	 * Gracefully notifies and disconnects a client.
	 * @param client the client to disconnect
	 * @param reason the reason for the disconnection
	 */
	private void disconnect(ClientState client, String reason) {
		sendReliable(client, buildServerDisconnect(reason), false);
		removeClient(client);
		broadcastAndPrint(getForcefulDisconnectMessage(client, reason));
	}
	
	/**
	 * Gracefully notifies and disconnects all connected clients.
	 * @param reason the reason for the disconnection
	 */
	public void disconnectAll(String reason) {
		int count = clients.size();
		synchronized(clients) {
			broadcastReliable(buildServerDisconnect(reason), false);
			for(ClientState client : clients.values()) {
				network.removeConnection(client.address);
				if(client.player != null) {
					world.remove(client.player);
					sendRemoveEntity(client.player);
				}
			}
			clients.clear();
		}
		if(count > 0)
			System.out.println("Disconnected " + count + " remaining clients");
	}
	
	/**
	 * Removes all of a client's state from the server.
	 * @param client the client to remove from the server.
	 */
	private void removeClient(ClientState client) {
		if(clients.remove(client.address) == null)
			throw new IllegalArgumentException(client + " is not connected to the server");
		network.removeConnection(client.address);
		if(client.player != null) {
			processQueue.add(()->{
				world.remove(client.player);
				byte[] packet = new byte[2 + 4];
				ByteUtil.putShort(packet, 0, Protocol.SERVER_REMOVE_ENTITY);
				ByteUtil.putInteger(packet, 2, client.player.getID());
				broadcastReliable(packet, true, c -> !c.equals(client));
			});
		}
			
	}
	
	private String getForcefulDisconnectMessage(ClientState client, String reason) {
		return client.getAddressString() + " was disconnected ("
				+ (reason != null && reason.length() > 0 ? "reason: " + reason + ", ": "") 
				+ clients.size() + " players connected)";
	}
	
	private String getClientDisconnectMessage(ClientState client) {
		return client.getAddressString() + " disconnected (" + clients.size() + " players connected)";
	}
	
	private void broadcastAndPrint(String consoleMessage) {
		broadcastReliable(Protocol.buildConsoleMessage(consoleMessage), true);
		System.out.println(consoleMessage);
	}
	
	private void sendClientConnectReply(InetSocketAddress client, boolean connected) {
		byte[] response = new byte[3];
		ByteUtil.putShort(response, 0, Protocol.SERVER_CONNECT_ACKNOWLEDGMENT);
		ByteUtil.putBoolean(response, 2, connected);
		try {
			network.sendReliable(client, response, 10, 100);
		} catch (TimeoutException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private void connectClient(InetSocketAddress address) {
		try {
			sendClientConnectReply(address, canConnect);
			if(canConnect) {
				processQueue.add(()->{
					//construct a new player for the client
					PlayerEntity player = new PlayerEntity(world.nextEntityID());
					
					//set the player's position to directly above the ground in the center of the world
					BlockGrid grid = world.getForeground();
					float posX = world.getForeground().getWidth()/2;
					player.setPositionX(posX);
					for(int i = grid.getHeight() - 2; i > 1; i--) {
						if(grid.get(posX, i) == null && grid.get(posX, i + 1) == null && grid.get(posX, i - 1) != null) {
							player.setPositionY(i);
							break;
						}
					}
					
					world.add(player);
					sendAddEntity(player); //send entity to already connected players
					
					//create the client's ClientState object to track their information
					ClientState client = new ClientState(address, "noname", player);
					
					//send the world to the client
					try {
						for(byte[] packet : buildWorldPackets(world)) {
							sendReliableThrow(client, packet);
						}
						
						System.out.println("Sent all world packets to " + client.getAddressString());
						clients.put(address, client);
						sendPlayerID(player, client); //send id of player entity (which was sent in world data)
						System.out.println(client.getAddressString() + " joined (" + clients.size() + " players connected)");
					} catch(TimeoutException e) {
						world.remove(player);
						sendRemoveEntity(player);
						System.out.println(client.getAddressString() + " timed out while connecting");
					}
				});
			}
		} catch(TimeoutException e) {
			System.out.println(address + " attempted to connect, but timed out");
		}
	}
	
	private static byte[][] buildWorldPackets(World world) {
		//serialize the world for transfer
		byte[] worldBytes = Protocol.COMPRESS_WORLD_DATA ? ByteUtil.compress(SerializationProvider.getProvider().serialize(world)) : SerializationProvider.getProvider().serialize(world);
		
		//build packets
		byte[][] packets = ByteUtil.split(worldBytes, Protocol.MAX_MESSAGE_LENGTH - 2, 2, 1);
		for(int i = 1; i < packets.length; i++) {
			ByteUtil.putShort(packets[i], 0, Protocol.SERVER_WORLD_DATA);
		}
	
		//create the first packet to send, which contains the number of subsequent packets
		byte[] head = new byte[6];
		ByteUtil.putShort(head, 0, Protocol.SERVER_WORLD_HEAD);
		ByteUtil.putInteger(head, 2, worldBytes.length);
		packets[0] = head;
		//System.out.println("World packets " + packets.length + ", " + worldBytes.length);
		return packets;
	}
	
	private void broadcastReliable(byte[] data, boolean removeUnresponsive) {
		broadcastReliable(data, removeUnresponsive, c -> true);
	}
	
	/**
	 * Broadcasts {@code data} to each client connected to this Server and returns once all clients have received the data or
	 * become unresponsive
	 * @param data the packet of data to send.
	 * @param removeUnresponsive whether or not to remove clients that do not respond from the server
	 * @param sendTest specify which clients the message should be sent to and which should not
	 */
	private void broadcastReliable(byte[] data, boolean removeUnresponsive, Predicate<ClientState> sendTest) {
		//TODO need alternative to synchronizing over all clients (separate lock to disallow add during broadcast?
		if(clients.size() > 1) {
			CountDownLatch barrier = new CountDownLatch(clients.size());
			clients.forEach((address, client) -> {
				if(sendTest.test(client)) {
					broadcastPool.execute(() -> {
						try {
							sendReliable(client, data, removeUnresponsive);
						} finally {
							barrier.countDown();
						}
					});
				} else {
					barrier.countDown();
				}
			});
			
			try {
				barrier.await(); //wait until an acknowledgement has been received from all clients
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else if(clients.size() == 1) { //if there is only one client, skip extra steps
			ClientState client;
			synchronized(clients) {
				client = clients.values().iterator().next();
			}
			if(sendTest.test(client))
				sendReliable(client, data, removeUnresponsive);
		} //else: no clients are connected, send nothing
	}
	
	@SuppressWarnings("unused")
	private void broadcastReliableAsync(byte[] data, boolean removeUnresponsive) {
		broadcastReliableAsync(data, removeUnresponsive, c -> true);
	}
	
	private void broadcastReliableAsync(byte[] data, boolean removeUnresponsive, Predicate<ClientState> sendTest) {
		clients.forEach((address, client) -> {
			if(sendTest.test(client)) {
				broadcastPool.execute(() -> sendReliable(client, data, removeUnresponsive));
			}
		});
	}
	
	@SuppressWarnings("unused")
	private void broadcastUnreliable(byte[] data) {
		broadcastUnreliable(data, c -> true);
	}
	
	/**
	 * Broadcasts {@code data} to each client connected to this Server and returns
	 * @param data the packet of data to send.
	 */
	private void broadcastUnreliable(byte[] data, Predicate<ClientState> sendTest) {
		synchronized(clients) {
			for(ClientState client : clients.values()) {
				if(sendTest.test(client)) {
					try {
						network.sendUnreliable(client.address, data);
					} catch (IOException e) {
						e.printStackTrace();
					}	
				}
			}
		}
	}
	
	private void sendReliableThrow(ClientState client, byte[] data) throws TimeoutException {
		try {
			long time = System.nanoTime();
			network.sendReliable(client.address, data, Protocol.RESEND_COUNT, Protocol.RESEND_INTERVAL);
			client.ping = Utility.nanosSince(time);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void sendReliable(ClientState client, byte[] data, boolean removeUnresponsive) {
		try {
			sendReliableThrow(client, data);
		} catch(TimeoutException e) {
			synchronized(client) { //prevent other threads from disconnecting client at the same time
				if(removeUnresponsive && isConnected(client.address)) {
					removeClient(client);
					System.out.println(getForcefulDisconnectMessage(client, "client unresponsive"));
				}	
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void sendReliableAsync(ClientState client, byte[] data, boolean removeUnresponsive) {
		broadcastPool.execute(() -> sendReliable(client, data, removeUnresponsive));
	}
	
	public static final class ClientState {
		public final InetSocketAddress address;
		private final AtomicInteger disconnectStrikes;
		public volatile PlayerEntity player;
		public volatile boolean worldSetupComplete;
		public final String username;
		public volatile long ping;
		
		public ClientState(InetSocketAddress address, String username, PlayerEntity player) {
			this.address = address;
			this.username = username;
			this.player = player;
			this.disconnectStrikes = new AtomicInteger();
		}
		
		byte strike() {
			return (byte)disconnectStrikes.incrementAndGet();
		}
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof ClientState)
				return address.equals(((ClientState)o).address);
			return false;
		}
		
		public String getAddressString() {
			return address.getAddress().getHostAddress();
		}
		
		@Override
		public String toString() {
			return username + "/" + address.getAddress().getHostAddress() + "/" + Utility.formatTime(ping);
		}
	}
}
