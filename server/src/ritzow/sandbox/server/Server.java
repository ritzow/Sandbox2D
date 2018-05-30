package ritzow.sandbox.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
	private final ExecutorService receivePool, broadcastPool;
	private final ServerRepeatUpdater updater;
	private final Map<InetSocketAddress, ClientState> clients;
	private volatile boolean canConnect;
	
	public static Server open(InetSocketAddress bindAddress) throws IOException {
		return new Server(bindAddress);
	}
	
	private Server(InetSocketAddress bindAddress) throws IOException {
		this.network = new NetworkController(bindAddress, this::process);
		ThreadGroup processors = new ThreadGroup("Message Processors");
		ThreadGroup senders = new ThreadGroup("Broadcaster Group");
		this.receivePool = Executors.newFixedThreadPool(10, 
				runnable -> new Thread(processors, runnable, "Processor"));
		AtomicInteger id = new AtomicInteger();
		this.broadcastPool = Executors.newCachedThreadPool(
				runnable -> new Thread(senders, runnable, "Broadcaster " + id.incrementAndGet()));
		this.clients = Collections.synchronizedMap(new HashMap<InetSocketAddress, ClientState>());
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
	
	public ClientState[] listClients() {
		return clients.values().toArray(new ClientState[clients.size()]);
	}
	
	private void onReceive(ClientState client, short protocol, byte[] data) {
		switch(protocol) {
			case Protocol.CLIENT_DISCONNECT: //this will make clients "unresponsive" still
				removeClient(client);
				System.out.println(getClientDisconnectMessage(client));
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
		receivePool.execute(() -> {
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
			updater.resetEntitySend(); //reset all entities when a player 'changes direction'
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
	
	public void sendUpdateEntity(Entity e) {
		broadcastUnreliable(buildUpdateEntity(e));
	}
	
	private static byte[] buildUpdateEntity(Entity e) {
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
					network.sendUnreliable(client.address, data);
			}
		}
	}
	
	private void sendReliable(ClientState client, byte[] data, boolean removeUnresponsive) {
		try {
			long time = System.nanoTime();
			network.sendReliable(client.address, data, 10, 100);
			client.ping = (int)Utility.millisSince(time); //update client ping
		} catch(TimeoutException e) {
			if(removeUnresponsive && isConnected(client.address)) {
				removeClient(client);
				System.out.println(getForcefulDisconnectMessage(client, "client unresponsive"));
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void sendUnreliable(ClientState client, byte[] data) {
		network.sendUnreliable(client.address, data);
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
		System.out.println(getForcefulDisconnectMessage(client, reason));
	}
	
	/**
	 * Removes all of a client's state from the server.
	 * @param client the client to remove from the server.
	 */
	private void removeClient(ClientState client) {
		if(clients.remove(client.address) == null)
			throw new IllegalStateException(client + " is not connected to the server");
		network.removeConnection(client.address);
		if(client.player != null) updater.submitTask(() -> {
			updater.getWorld().remove(client.player);
			byte[] packet = new byte[2 + 4];
			ByteUtil.putShort(packet, 0, Protocol.SERVER_REMOVE_ENTITY);
			ByteUtil.putInteger(packet, 2, client.player.getID());
			broadcastReliable(packet, true, c -> !c.equals(client));
		});
	}
	
	private String getForcefulDisconnectMessage(ClientState client, String reason) {
		return client.getAddressString() + " was disconnected ("
				+ (reason != null && reason.length() > 0 ? "reason: " + reason + ", ": "") 
				+ clients.size() + " players connected)";
	}
	
	private String getClientDisconnectMessage(ClientState client) {
		return client.getAddressString() + " disconnected (" + clients.size() + " players connected)";
	}
	
	private void removePlayer(Entity player) {
		updater.submitTask(() -> {
			updater.getWorld().remove(player);
			sendRemoveEntity(player);
		});
	}
	
	public void disconnectAll(String reason) {
		int count = clients.size();
		synchronized(clients) {
			broadcastReliable(buildServerDisconnect(reason), false);
			for(ClientState client : clients.values()) {
				network.removeConnection(client.address);
				if(client.player != null) {
					removePlayer(client.player);
				}
			}
			clients.clear();
		}
		if(count > 0)
			System.out.println("Disconnected " + count + " remaining clients");
	}
	
	private void sendClientConnectReply(InetSocketAddress client, int messageID, boolean connected) {
		byte[] response = new byte[3];
		ByteUtil.putShort(response, 0, Protocol.SERVER_CONNECT_ACKNOWLEDGMENT);
		ByteUtil.putBoolean(response, 2, connected);
		network.sendReliable(client, response, 10, 100);
	}
	
	private void connectClient(InetSocketAddress address) {
		try {
			//determine if client can connect, and send a response
			boolean canConnect = this.canConnect; //TODO add other connection conditions
			sendClientConnectReply(address, 0, canConnect);
			
			if(canConnect) {
				//create the client's ClientState object to track their information
				ClientState newClient = new ClientState(address);
				updater.submitTask(() -> {
					World world = updater.getWorld();
					
					//construct a new player for the client
					PlayerEntity player = new PlayerEntity(world.nextEntityID());
					newClient.player = player;
					
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
					
					//send the world to the client
					for(byte[] packet : buildWorldPackets(world)) {
						sendReliable(newClient, packet, true);
					}
					
					clients.put(address, newClient);
					sendPlayerID(player, newClient); //send id of player entity (which was sent in world data)
					System.out.println(newClient.getAddressString()
							+ " joined (" + clients.size() + " players connected)");
				});
			}
		} catch(TimeoutException e) {
			System.out.println(address + " attempted to connect, but timed out");
		}
	}
	
	public byte[][] buildWorldPackets(World world) {
		//serialize the world for transfer
		byte[] worldBytes = ByteUtil.compress(SerializationProvider.getProvider().serialize(world));
		
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
		
		return packets;
	}
	
	public static final class ClientState {
		private final InetSocketAddress address;
		private volatile String username;
		private volatile PlayerEntity player;
		private final AtomicInteger disconnectStrikes;
		private volatile int ping; //set ping on receive message
		
		private static final AtomicInteger playerID = new AtomicInteger(1);
		
		public ClientState(InetSocketAddress address) {
			this.disconnectStrikes = new AtomicInteger();
			this.address = address;
			username = "player" + playerID.getAndIncrement();
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
			return username + "/" + address.getAddress().getHostAddress() + "/" + ping + "ms";
		}
	}
}
