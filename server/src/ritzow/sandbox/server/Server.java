package ritzow.sandbox.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
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

public class Server {
	private final NetworkController network;
	private final ExecutorService worker, broadcaster;
	private final ServerRepeatUpdater updater;
	private final SerializerReaderWriter serialRegistry;
	private final Map<InetSocketAddress, ClientState> clients;
	private volatile boolean canConnect;
	
	public Server(int port) throws SocketException, UnknownHostException {
		this(new InetSocketAddress(InetAddress.getLocalHost(), port));
	}
	
	public Server(InetSocketAddress bindAddress) throws SocketException {
		this.network = new NetworkController(bindAddress, this::process);
		ThreadGroup processors = new ThreadGroup("Message Processors");
		ThreadGroup senders = new ThreadGroup("Broadcaster Group");
		this.worker = Executors.newCachedThreadPool(runnable -> new Thread(processors, runnable));
		this.broadcaster = Executors.newCachedThreadPool(runnable -> new Thread(senders, runnable));
		this.clients = Collections.synchronizedMap(new HashMap<InetSocketAddress, ClientState>());
		this.serialRegistry = SerializationProvider.getProvider();
		this.updater = new ServerRepeatUpdater(this);
	}
	
	public void start(World world) {
		world.setRemoveEntities(this::sendRemoveEntity);
		updater.startWorld(world);
		updater.start("Game Updater");
		network.start();
		canConnect = true;
	}
	
	public void stop() {
		canConnect = false;
		disconnectAll("server shutting down");
		updater.stop();
		worker.shutdown();
		broadcaster.shutdown();
		network.stop();
		try {
			broadcaster.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			worker.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public ClientState[] listClients() {
		ClientState[] list = new ClientState[clients.size()];
		return clients.values().toArray(list);
	}
	
	private void onReceive(ClientState client, short protocol, byte[] data) {
		switch(protocol) {
			case Protocol.CLIENT_DISCONNECT:
				disconnect(client, false);
				break;
			case Protocol.CLIENT_PLAYER_ACTION:
				processPlayerAction(client, data);
				break;
			case Protocol.CLIENT_BREAK_BLOCK:
				processClientBreakBlock(client, data);
				break;
			default:
				throw new ClientBadDataException("received unknown protocol " + protocol);
		}
	}
	
	private void process(InetSocketAddress sender, int messageID, byte[] data) {
		worker.execute(() -> {
			short protocol = ByteUtil.getShort(data, 0);
			ClientState client = clients.get(sender);
			try {
				if(client == null && protocol == Protocol.CLIENT_CONNECT_REQUEST) {
					connectClient(sender);
				} else if(client != null && protocol != Protocol.CLIENT_CONNECT_REQUEST) {
					onReceive(client, protocol, data);
				}
			} catch(ClientBadDataException e) {
				//disconnect the client if it sends invalid data three times or more
				if(client != null && client.strike() >= 3) {
					disconnect(client, "server received bad data from client 3 times");
				}
			}
		});
	}
	

	private final void processPlayerAction(ClientState client, byte[] data) {
		if(client.player == null)
			throw new ClientBadDataException("client has no associated player to perform an action");
		updater.submitTask(() -> {
			PlayerAction action = PlayerAction.forCode(data[2]);
			boolean enable = ByteUtil.getBoolean(data, 3);
			client.player.processAction(action, enable);
		});
	}
	
	private void processClientBreakBlock(ClientState client, byte[] data) {
		int x = ByteUtil.getInteger(data, 2);
		int y = ByteUtil.getInteger(data, 6);
		if(!updater.getWorld().getForeground().isValid(x, y))
			throw new ClientBadDataException("client sent bad x and y block coordinates");
		
		updater.submitTask(() -> {
			if(updater.getWorld() != null) {
				Block block = updater.getWorld().getForeground().get(x, y);
				if(updater.getWorld().getForeground().destroy(updater.getWorld(), x, y)) {
					//TODO block data needs to be reset on drop
					ItemEntity<BlockItem> drop = 
							new ItemEntity<>(updater.getWorld().nextEntityID(), new BlockItem(block), x, y);
					drop.setVelocityX(-0.2f + ((float) Math.random() * (0.4f)));
					drop.setVelocityY((float) Math.random() * (0.35f));
					updater.getWorld().add(drop);
					sendRemoveBlock(x, y);
					sendAddEntity(drop);
				}
			}
		});
	}
	
	public void broadcastPing() {
		byte[] packet = new byte[2];
		ByteUtil.putShort(packet, 0, Protocol.PING);
		broadcastReliable(packet, true);
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
		byte[] entity = serialRegistry.serialize(e);
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
	
	public void sendUpdateEntity(Entity e) {
		//protocol, id, posX, posY, velX, velY
		byte[] update = new byte[2 + 4 + 4 + 4 + 4 + 4];
		ByteUtil.putShort(update, 0, Protocol.SERVER_ENTITY_UPDATE);
		ByteUtil.putInteger(update, 2, e.getID());
		ByteUtil.putFloat(update, 6, e.getPositionX());
		ByteUtil.putFloat(update, 10, e.getPositionY());
		ByteUtil.putFloat(update, 14, e.getVelocityX());
		ByteUtil.putFloat(update, 18, e.getVelocityY());
		broadcastUnreliable(update);
	}
	
	private static byte[] buildServerDisconnect(String reason) {
		byte[] message = reason.getBytes(Protocol.CHARSET);
		byte[] packet = new byte[message.length + 6];
		ByteUtil.putShort(packet, 0, Protocol.SERVER_CLIENT_DISCONNECT);
		ByteUtil.putInteger(packet, 2, message.length);
		ByteUtil.copy(message, packet, 6);
		return packet;
	}
	
	public void broadcastConsoleMessage(String message) {
		broadcastReliable(Protocol.buildConsoleMessage(message), true);
	}
	
	private void broadcastReliable(byte[] data, boolean removeUnresponsive) {
		broadcastReliable(data, removeUnresponsive, c -> true);
	}
	
	/**
	 * Broadcasts {@code data} to each client connected to this Server and returns once all clients have received the data or
	 * become unresponsive
	 * @param data the packet of data to send.
	 * @param sendTest specify which clients the message should be sent to and which should not
	 * @param removeUnresponsive whether or not to remove clients that do not respond from the server
	 */
	private void broadcastReliable(byte[] data, boolean removeUnresponsive, Predicate<ClientState> sendTest) {
		synchronized(clients) { //synchronize entire method so clients cant connect mid-send
			if(clients.size() > 1) {
				CountDownLatch barrier = new CountDownLatch(clients.size());
				clients.forEach((address, client) -> {
					if(sendTest.test(client)) {
						broadcaster.execute(() -> {
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
				ClientState client = clients.values().iterator().next();
				if(sendTest.test(client))
					sendReliable(client, data, removeUnresponsive);
			} //else: no clients are connected, send nothing
		}
	}
	
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
				if(sendTest.test(client))
					network.sendUnreliable(client.address, client.nextUnreliableID(), data);
			}
		}
	}
	
	private void sendReliable(ClientState client, byte[] data, boolean removeUnresponsive) {
		try {
			long time = System.nanoTime();
			network.sendReliable(client.address, client.nextReliableID(), data, 10, 100);
			client.ping = (int)(System.nanoTime() - time)/1_000_000; //update client ping
		} catch(TimeoutException e) {
			if(removeUnresponsive) {
				disconnect(client, false);
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void sendUnreliable(ClientState client, byte[] data) {
		network.sendUnreliable(client.address, client.nextUnreliableID(), data);
	}
	
	public boolean isConnected(ClientState client) {
		return clients.containsValue(client);
	}
	
	public boolean isConnected(InetSocketAddress address) {
		return clients.containsKey(address);
	}
	
	/**
	 * Disconnects a client from the server
	 * @param client the client to disconnect
	 * @param notifyClient whether or not to notify the client that is being disconnected
	 */
	private void disconnect(ClientState client, boolean notifyClient) {
		disconnect(client, notifyClient ? "" : null);
	}
	
	/**
	 * Disconnects a client from the server
	 * @param client the client to disconnect
	 * @param reason the reason for disconnecting the client, or null to not notify the client
	 */
	private void disconnect(ClientState client, String reason) {
		if(clients.remove(client.address) != null) {
			if(reason != null)
				sendReliable(client, buildServerDisconnect(reason), true);
			network.removeSender(client.address);
			if(client.player != null) {
				updater.submitTask(() -> removePlayer(client.player));
			}
			System.out.println(client + " disconnected (reason: " + reason + ", " + clients.size() + " players connected)");
		} else {
			System.out.println(client + " is not connected to the server");
		}
	}
	
	public void disconnectAll(String reason) {
		synchronized(clients) {
			//do not remove unresponsive clients here because they will always be unresponsive (they disconnected!)
			broadcastReliable(buildServerDisconnect(reason), false);
			
			for(ClientState client : clients.values()) {
				network.removeSender(client.address);
				if(client.player != null) {
					updater.submitTask(() -> removePlayer(client.player));
				}
			}
			clients.clear();
		}
	}
	
	private void removePlayer(Entity player) {
		updater.getWorld().remove(player);
		sendRemoveEntity(player);
	}
	
	private void sendClientConnectReply(InetSocketAddress client, int messageID, boolean connected) {
		byte[] response = new byte[3];
		ByteUtil.putShort(response, 0, Protocol.SERVER_CONNECT_ACKNOWLEDGMENT);
		ByteUtil.putBoolean(response, 2, canConnect);
		network.sendReliable(client, messageID, response, 10, 100);
	}
	
	private void connectClient(InetSocketAddress address) {
		try {
			//determine if client can connect, and send a response
			boolean canConnect = this.canConnect; //TODO add other connection conditions
			sendClientConnectReply(address, 0, canConnect);
			
			if(canConnect) {
				//create the client's ClientState object to track their information
				ClientState newClient = new ClientState(1, address);
				updater.submitTask(() -> {
					World world = updater.getWorld();
					
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
					sendAddEntity(player);
					
					//send the world to the client
					for(byte[] packet : buildWorldPackets(world, serialRegistry)) {
						sendReliable(newClient, packet, true);
					}
					
					clients.put(address, newClient);
					sendPlayerID(player, newClient);
					System.out.println(newClient + " joined (" + clients.size() + " players connected)");
				});
			}
		} catch(TimeoutException e) {
			System.out.println(address + " attempted to connect, but timed out");
		}
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
	
	public int getConnectedClients() {
		return clients.size();
	}
	
	public static final class ClientState {
		private final InetSocketAddress address;
		private final AtomicInteger reliableMessageID, unreliableMessageID;
		private volatile String username;
		private volatile PlayerEntity player;
		private final AtomicInteger disconnectStrikes;
		private volatile int ping; //set ping on receive message
		
		private static final AtomicInteger playerID = new AtomicInteger(1);
		
		public ClientState(int initReliable, InetSocketAddress address) {
			this.reliableMessageID = new AtomicInteger(initReliable);
			this.unreliableMessageID = new AtomicInteger();
			this.disconnectStrikes = new AtomicInteger();
			this.address = address;
			username = "player" + playerID.getAndIncrement();
		}
		
		int nextUnreliableID() {
			return unreliableMessageID.getAndIncrement();
		}
		
		int nextReliableID() {
			return reliableMessageID.getAndIncrement();
		}
		
		byte strike() {
			return (byte)disconnectStrikes.incrementAndGet();
		}
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof ClientState)
				return address.equals(((ClientState)o).address);
			else
				return false;
		}
		
		@Override
		public String toString() {
			return username + "/" + address.getAddress().getHostAddress() + "/" + ping + "ms";
		}
	}
}
