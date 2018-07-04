package ritzow.sandbox.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import ritzow.sandbox.data.ByteUtil;
import ritzow.sandbox.network.NetworkController;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.network.Protocol.PlayerAction;
import ritzow.sandbox.network.TimeoutException;
import ritzow.sandbox.server.world.entity.ServerBombEntity;
import ritzow.sandbox.server.world.entity.ServerPlayerEntity;
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
	private final ExecutorService workerPool;
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
		ThreadGroup processors = new ThreadGroup(this + " Server Workers");
		this.workerPool = Executors.newCachedThreadPool(
				runnable -> new Thread(processors, runnable, "Worker"));
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
		workerPool.shutdown();
		network.stop(); //TODO should network.stop go after workerPool.awaitTermination?
		try {
			workerPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void receive(InetSocketAddress sender, byte[] data) {
		short type = ByteUtil.getShort(data, 0);
		ClientState client = clients.get(sender);

		if(client == null && type == Protocol.CLIENT_CONNECT_REQUEST) {
			workerPool.execute(() -> connectClient(sender));
		} else if(client != null && type != Protocol.CLIENT_CONNECT_REQUEST) {
			workerPool.execute(() -> process(client, type, data));
		}
	}
	
	private void process(ClientState client, short type, byte[] data) {
		try {
			switch(type) {
				case Protocol.CLIENT_DISCONNECT:
					client.disconnect();
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
			disconnect(client, "Sent bad data");
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
		broadcastReliable(PING, true, client -> client.worldSetupComplete);
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
	
	private static void sendPlayerID(PlayerEntity player, ClientState recipient) {
		byte[] packet = new byte[6];
		ByteUtil.putShort(packet, 0, Protocol.SERVER_PLAYER_ID);
		ByteUtil.putInteger(packet, 2, player.getID());
		recipient.sendReliable(packet);
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
		//sendReliable(client, buildServerDisconnect(reason), false);
		client.sendReliable(buildServerDisconnect(reason));
		client.disconnect();
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
					PlayerEntity player = new ServerPlayerEntity(world.nextEntityID());
					
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
					ClientState client = new ClientState(address, player);
					
					//send the world to the client
					try {
						for(byte[] packet : buildWorldPackets(world)) {
							//sendReliableThrow(client, packet);
							client.sendReliable(packet);
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
		clients.forEach((address, client) -> {
			if(sendTest.test(client)) {
				client.sendReliable(data);	
			}
		});
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
	
	static class SendPacket {
		byte[] data;
		boolean reliable;
		
		SendPacket(byte[] data, boolean reliable) {
			this.data = data;
			this.reliable = reliable;
		}
	}
	
	final class ClientState {
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
			(sendQueueThread = new Thread(this::runSender)).start();
		}
		
		public String getAddressString() {
			return address.getAddress().getHostAddress();
		}
		
		public void disconnect() {
			//stop the send thread
			exit = true;
			sendQueueThread.interrupt();
			
			//remove from the client list
			if(clients.remove(address) == null)
				throw new IllegalArgumentException(getAddressString() + " is not connected to the server");
			
			//remove from the network controller
			network.removeConnection(address);
			
			//remove player from world
			if(player != null) {
				processQueue.add(() -> {
					world.remove(player);
					byte[] packet = new byte[2 + 4];
					ByteUtil.putShort(packet, 0, Protocol.SERVER_REMOVE_ENTITY);
					ByteUtil.putInteger(packet, 2, player.getID());
					broadcastReliable(packet, true, c -> !c.equals(this));
				});
			}
		}
		
		public void sendReliable(byte[] data) {
			sendQueue.add(new SendPacket(data, true));
		}
		
		public void sendUnreliable(byte[] data) {
			sendQueue.add(new SendPacket(data, false));
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
				} catch (InterruptedException e) {
					if(exit) break;
				} catch (TimeoutException e) {
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
