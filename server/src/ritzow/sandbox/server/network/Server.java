package ritzow.sandbox.server.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.network.NetworkController;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.network.Protocol.PlayerAction;
import ritzow.sandbox.network.TimeoutException;
import ritzow.sandbox.server.SerializationProvider;
import ritzow.sandbox.server.world.entity.ServerPlayerEntity;
import ritzow.sandbox.util.SharedConstants;
import ritzow.sandbox.util.TaskQueue;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.entity.PlayerEntity;
import ritzow.sandbox.world.item.BlockItem;

public class Server {
	private final NetworkController network;
	private final ExecutorService workerPool;
	private final Map<InetSocketAddress, ClientState> clients;
	private volatile boolean canConnect, running;
	private long lastWorldUpdateTime, lastEntitySendTime;
	private World world;
	private final TaskQueue processQueue;
	
	private static final long NETWORK_SEND_INTERVAL_NANOSECONDS = Utility.millisToNanos(150);
	
	public static Server open(InetSocketAddress bindAddress) throws IOException {
		return new Server(bindAddress);
	}
	
	private Server(InetSocketAddress bindAddress) throws IOException {
		this.network = new NetworkController(Utility.getProtocolFamily(bindAddress.getAddress()), bindAddress, this::receive);
		ThreadGroup processors = new ThreadGroup(this + " Server Workers");
		this.workerPool = Executors.newCachedThreadPool(
				runnable -> new Thread(processors, runnable, "Worker"));
		this.clients = new ConcurrentHashMap<>();
		this.processQueue = new TaskQueue();
	}
	
	public ClientState[] listClients() {
		return clients.values().toArray(size -> new ClientState[size]);
	}
	
	public void start(World world) {
		this.world = world;
		world.setRemoveEntities(this::sendRemoveEntity);
		lastWorldUpdateTime = System.nanoTime();
		lastEntitySendTime = lastWorldUpdateTime;
		network.start();
		canConnect = true;
		running = true;
	}
	
	public InetSocketAddress getBindAddress() {
		return network.getBindAddress();
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void stop() {
		running = false;
		canConnect = false;
		disconnectAll("server shutting down");
		workerPool.shutdown();
		network.stop(); //TODO should network.stop go after workerPool.awaitTermination?
		try {
			workerPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void receive(InetSocketAddress sender, byte[] data) {
		short type = Bytes.getShort(data, 0);
		ClientState client = clients.get(sender);

		if(client == null && type == Protocol.TYPE_CLIENT_CONNECT_REQUEST) {
			workerPool.execute(() -> connectClient(sender));
		} else if(client != null && type != Protocol.TYPE_CLIENT_CONNECT_REQUEST) {
			workerPool.execute(() -> process(client, type, data));
		}
	}
	
	private void process(ClientState client, short type, byte[] data) {
		try {
			switch(type) {
				case Protocol.TYPE_CLIENT_DISCONNECT:
					client.disconnect();
					broadcastAndPrint(getClientDisconnectMessage(client));
					break;
				case Protocol.TYPE_CLIENT_PLAYER_ACTION:
					processPlayerAction(client, data);
					break;
				case Protocol.TYPE_CLIENT_BREAK_BLOCK:
					processClientBreakBlock(client, data);
					break;
				case Protocol.TYPE_CLIENT_BOMB_THROW:
//					processClientThrowBomb(client, data);
					break;
				case Protocol.TYPE_CLIENT_WORLD_BUILT:
					client.worldSetupComplete = true;
					break;
				default:
					throw new ClientBadDataException("received unknown protocol " + type);
			}
		} catch(ClientBadDataException e) {
			disconnect(client, "Sent bad data");
		}
	}
	
//	private static final float BOMB_THROW_VELOCITY = 0.8f;
//	
//	private void processClientThrowBomb(ClientState client, byte[] data) {
//		processQueue.add(() -> {
//			BombEntity bomb = new ServerBombEntity(this, world.nextEntityID());
//			bomb.setPositionX(client.player.getPositionX());
//			bomb.setPositionY(client.player.getPositionY());
//			float angle = Bytes.getFloat(data, 2);
//			bomb.setVelocityX((float) (Math.cos(angle) * BOMB_THROW_VELOCITY));
//			bomb.setVelocityY((float) (Math.sin(angle) * BOMB_THROW_VELOCITY));
//			world.add(bomb);
//			sendAddEntity(bomb);
//		});
//	}
	
	private final byte[] update; {
		update = new byte[2 + 4 + 4 + 4 + 4 + 4]; //protocol, id, posX, posY, velX, velY
		Bytes.putShort(update, 0, Protocol.TYPE_SERVER_ENTITY_UPDATE);
	}

	public void update() {
		processQueue.run();
		lastWorldUpdateTime = Utility.updateWorld(world, lastWorldUpdateTime, 
				SharedConstants.MAX_TIMESTEP, SharedConstants.TIME_SCALE_NANOSECONDS);
		if(Utility.nanosSince(lastEntitySendTime) > NETWORK_SEND_INTERVAL_NANOSECONDS) {
			byte[] update = this.update;
			for(Entity e : world) {
				Bytes.putInteger(update, 2, e.getID());
				Bytes.putFloat(update, 6, e.getPositionX());
				Bytes.putFloat(update, 10, e.getPositionY());
				Bytes.putFloat(update, 14, e.getVelocityX());
				Bytes.putFloat(update, 18, e.getVelocityY());
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
			boolean enable = Bytes.getBoolean(data, 3);
			client.player.processAction(action, enable);
			broadcastPlayerAction(client.player, action, enable);
		});
	}
	
	private void broadcastPlayerAction(PlayerEntity player, PlayerAction action, boolean isEnabled) {
		byte[] packet = new byte[8];
		Bytes.putShort(packet, 0, Protocol.TYPE_SERVER_PLAYER_ACTION);
		Bytes.putInteger(packet, 2, player.getID());
		packet[6] = action.getCode();
		Bytes.putBoolean(packet, 7, isEnabled);
		broadcastReliable(packet, client -> true, null);
	}
	
	private void processClientBreakBlock(ClientState client, byte[] data) {
		int x = Bytes.getInteger(data, 2);
		int y = Bytes.getInteger(data, 6);
		if(!world.getForeground().isValid(x, y))
			throw new ClientBadDataException("client sent bad x and y block coordinates");
		processQueue.add(() -> {
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
	
	private static final byte[] PING; static {
		PING = new byte[2];
		Bytes.putShort(PING, 0, Protocol.TYPE_SERVER_PING);
	}
	
	public void pingClients() {
		broadcastReliable(PING, client -> true, null);
	}
	
	public void broadcastConsoleMessage(String message) {
		broadcastReliable(Protocol.buildConsoleMessage(message), client -> true, null);
	}
	
	public void sendRemoveBlock(int x, int y) {
		byte[] packet = new byte[10];
		Bytes.putShort(packet, 0, Protocol.TYPE_SERVER_REMOVE_BLOCK);
		Bytes.putInteger(packet, 2, x);
		Bytes.putInteger(packet, 6, y);
		broadcastReliable(packet, client -> true, null);
	}
	
	private static void sendPlayerID(PlayerEntity player, ClientState client) {
		byte[] packet = new byte[6];
		Bytes.putShort(packet, 0, Protocol.TYPE_SERVER_PLAYER_ID);
		Bytes.putInteger(packet, 2, player.getID());
		client.send(packet, true, null, null);
	}
	
	public void sendAddEntity(Entity e) {
		byte[] entity = SerializationProvider.getProvider().serialize(e);
		boolean compress = entity.length > Protocol.MAX_MESSAGE_LENGTH - 3;
		entity = compress ? Bytes.compress(entity) : entity;
		byte[] packet = new byte[3 + entity.length];
		Bytes.putShort(packet, 0, Protocol.TYPE_SERVER_ADD_ENTITY);
		Bytes.putBoolean(packet, 2, compress);
		Bytes.copy(entity, packet, 3);
		broadcastReliable(packet, client -> true, null);
	}
	
	public void sendRemoveEntity(Entity e) {
		byte[] packet = new byte[2 + 4];
		Bytes.putShort(packet, 0, Protocol.TYPE_SERVER_REMOVE_ENTITY);
		Bytes.putInteger(packet, 2, e.getID());
		broadcastReliable(packet, client -> true, null);
	}
	
	private static byte[] buildServerDisconnect(String reason) {
		byte[] message = reason.getBytes(Protocol.CHARSET);
		byte[] packet = new byte[message.length + 6];
		Bytes.putShort(packet, 0, Protocol.TYPE_SERVER_CLIENT_DISCONNECT);
		Bytes.putInteger(packet, 2, message.length);
		Bytes.copy(message, packet, 6);
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
		//sendReliable(client, buildServerDisconnect(reason), false);
		var latch = new CountDownLatch(clients.size());
		client.send(buildServerDisconnect(reason), true, latch, null);
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		client.disconnect();
		broadcastAndPrint(getForcefulDisconnectMessage(client, reason));
	}
	
	/**
	 * Gracefully notifies and disconnects all connected clients.
	 * @param reason the reason for the disconnection
	 * @return the number of clients disconnected
	 */
	public int disconnectAll(String reason) {
		throw new UnsupportedOperationException("not implemented");
		//broadcastReliable(buildServerDisconnect(reason), client -> true, null);
		//TODO remove all client state after waiting for broadcast to complete
		//return 0;
	}
	
	private String getForcefulDisconnectMessage(ClientState client, String reason) {
		return Utility.formatAddress(client.address) + " was disconnected ("
				+ (reason != null && reason.length() > 0 ? "reason: " + reason + ", ": "") 
				+ clients.size() + " players connected)";
	}
	
	private String getClientDisconnectMessage(ClientState client) {
		return Utility.formatAddress(client.address) + " disconnected (" + clients.size() + " players connected)";
	}
	
	private void broadcastAndPrint(String consoleMessage) {
		broadcastReliable(Protocol.buildConsoleMessage(consoleMessage), client -> true, null);
		System.out.println(consoleMessage);
	}
	
	private void sendClientConnectReply(InetSocketAddress client, boolean connected) throws TimeoutException {
		byte[] response = new byte[3];
		Bytes.putShort(response, 0, Protocol.TYPE_SERVER_CONNECT_ACKNOWLEDGMENT);
		Bytes.putBoolean(response, 2, connected);
		try {
			network.sendReliable(client, response, 10, 100);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void connectClient(InetSocketAddress address) {
		try {
			sendClientConnectReply(address, canConnect);
			if(canConnect) {
				processQueue.add(() -> {
					//construct a new player for the client
					PlayerEntity player = new ServerPlayerEntity(world.nextEntityID());
					
					world.add(player);
					sendAddEntity(player); //send entity to already connected players
					
					//create the client's ClientState object to track their information
					ClientState client = new ClientState(address, player);
					
					//set the player's position to directly above the ground in the center of the world
					placePlayerInCenter(player, world.getForeground());
					
					//send the world to the client
					for(byte[] packet : buildWorldPackets(world)) {
						client.send(packet, true, null, null);
					}
					
					clients.put(address, client);
					sendPlayerID(player, client); //send id of player entity (which was sent in world data)
					System.out.println(Utility.formatAddress(address) + " joined (" + getConnectedClients() + " players connected)");
				});
			}
		} catch(TimeoutException e) {
			System.out.println(Utility.formatAddress(address) + " attempted to connect, but timed out");
		}
	}
	
	private static void placePlayerInCenter(PlayerEntity player, BlockGrid grid) {
		float posX = grid.getWidth()/2;
		player.setPositionX(posX);
		for(int i = grid.getHeight() - 2; i > 1; i--) {
			if(grid.get(posX, i) == null && grid.get(posX, i + 1) == null && grid.get(posX, i - 1) != null) {
				player.setPositionY(i);
				break;
			}
		}
	}
	
	private static byte[][] buildWorldPackets(World world) {
		//serialize the world for transfer
		byte[] worldBytes = Protocol.COMPRESS_WORLD_DATA ? Bytes.compress(SerializationProvider.getProvider().serialize(world)) : SerializationProvider.getProvider().serialize(world);
		
		//build packets
		byte[][] packets = Bytes.split(worldBytes, Protocol.MAX_MESSAGE_LENGTH - 2, 2, 1);
		for(int i = 1; i < packets.length; i++) {
			Bytes.putShort(packets[i], 0, Protocol.TYPE_SERVER_WORLD_DATA);
		}
	
		//create the first packet to send, which contains the number of subsequent packets
		byte[] head = new byte[6];
		Bytes.putShort(head, 0, Protocol.TYPE_SERVER_WORLD_HEAD);
		Bytes.putInteger(head, 2, worldBytes.length);
		packets[0] = head;
		return packets;
	}
	
	/**
	 * Broadcasts {@code data} to each client connected to this Server and returns once all clients have received the data or
	 * become unresponsive
	 * @param data the packet of data to send.
	 * @param removeUnresponsive whether or not to remove clients that do not respond from the server
	 * @param sendTest specify which clients the message should be sent to and which should not
	 */
	private void broadcastReliable(byte[] data, Predicate<ClientState> sendTest, Consumer<ClientState> responseAction) {
		clients.forEach((address, client) -> {
			if(sendTest.test(client)) {
				client.send(data, true, null, responseAction == null ? null : () -> responseAction.accept(client));
			}
		});
	}
	
	/**
	 * Broadcasts {@code data} to each client connected to this Server and returns
	 * @param data the packet of data to send.
	 */
	private void broadcastUnreliable(byte[] data, Predicate<ClientState> sendTest) {
		clients.forEach((address, client) -> {
			if(sendTest.test(client)) {
				client.send(data, false, null, null);
			}
		});
	}
	
	private static class SendPacket {
		byte[] data;
		boolean reliable;
		Runnable action;
		CountDownLatch sync;
		
		SendPacket(byte[] data, boolean reliable, Runnable postAction, CountDownLatch sync) {
			this.data = data;
			this.reliable = reliable;
			this.action = postAction;
			this.sync = sync;
		}
	}
	
	public final class ClientState {
		final InetSocketAddress address;
		final BlockingQueue<SendPacket> sendQueue;
		final Thread sendQueueThread;
		volatile PlayerEntity player;
		volatile boolean worldSetupComplete, exit;
		volatile long ping;
		
		public ClientState(InetSocketAddress address, PlayerEntity player) {
			this.address = address;
			this.player = player;
			this.sendQueue = new LinkedBlockingQueue<SendPacket>();
			(sendQueueThread = new Thread(this::runSender, address + " Client Sender")).start();
		}
		
		public void disconnect() {
			//stop the send thread
			exit = true;
			sendQueueThread.interrupt();
			
			//remove from the client list
			if(clients.remove(address) == null)
				throw new IllegalArgumentException(Utility.formatAddress(address) + " is not connected to the server");
			
			//remove from the network controller
			network.removeConnection(address);
			
			//remove player from world
			if(player != null) {
				processQueue.add(() -> {
					world.remove(player);
					byte[] packet = new byte[2 + 4];
					Bytes.putShort(packet, 0, Protocol.TYPE_SERVER_REMOVE_ENTITY);
					Bytes.putInteger(packet, 2, player.getID());
					broadcastReliable(packet, client -> !equals(client), null);
				});
			}
		}
		
		public void send(byte[] data, boolean reliable, CountDownLatch wait, Runnable postAction) {
			sendQueue.add(new SendPacket(data, reliable, postAction, wait));
		}
		
		private void sendReliableImpl(byte[] data) {
			try {
				try {
					long time = System.nanoTime();
					network.sendReliable(address, data, Protocol.RESEND_COUNT, Protocol.RESEND_INTERVAL);
					ping = Utility.nanosSince(time);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch(TimeoutException e) {
				synchronized(this) { //prevent other threads from disconnecting this at the same time
					if(isConnected(address)) {
						disconnect();
						System.out.println(getForcefulDisconnectMessage(this, "client unresponsive"));
					}	
				}
			}
		}
		
		private void runSender() {
			while(true) {
				try {
					SendPacket packet = sendQueue.take();
					if(packet.reliable) {
						sendReliableImpl(packet.data); //TODO what happens if interrupted while waiting here
					} else {
						network.sendUnreliable(address, packet.data);
					}
					if(packet.action != null)
						packet.action.run();
					if(packet.sync != null)
						packet.sync.countDown();
				} catch (InterruptedException e) {
					if(exit) break;
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		@Override
		public String toString() {
			return address.getAddress().getHostAddress() + "/" + Utility.formatTime(ping);
		}
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof ClientState)
				return address.equals(((ClientState)o).address);
			return false;
		}
	}
}
