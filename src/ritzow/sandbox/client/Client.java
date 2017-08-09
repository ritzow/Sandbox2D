package ritzow.sandbox.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.function.Supplier;
import ritzow.sandbox.client.world.ClientWorld;
import ritzow.sandbox.game.Lobby;
import ritzow.sandbox.protocol.ConnectionException;
import ritzow.sandbox.protocol.NetworkController;
import ritzow.sandbox.protocol.Protocol;
import ritzow.sandbox.protocol.TimeoutException;
import ritzow.sandbox.protocol.WorldObjectIdentifiers;
import ritzow.sandbox.util.ByteUtil;
import ritzow.sandbox.util.SerializerReaderWriter;
import ritzow.sandbox.util.Transportable;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.DirtBlock;
import ritzow.sandbox.world.block.GrassBlock;
import ritzow.sandbox.world.component.Inventory;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.entity.ParticleEntity;
import ritzow.sandbox.world.entity.PlayerEntity;
import ritzow.sandbox.world.item.BlockItem;

public final class Client {
	
	/** The server's address **/
	protected volatile SocketAddress server;
	
	/** I/O logic **/
	protected final NetworkController controller;
	
	/** if the server has been successfully connected to **/
	protected volatile boolean connected;
	
	/** messageID **/
	protected volatile int unreliableMessageID, reliableMessageID;
	
	/** The World object sent by the server **/
	protected volatile ClientWorld world;
	private byte[][] worldPackets;
	
	/** The Player object sent by the server for the client to control **/
	protected volatile PlayerEntity player;
	
	/** The Lobby object sent by the server for the client to display **/
	protected Lobby lobby;
	
	/** lock object for synchronizing server-initiated actions with the client **/
	protected final Object worldLock, playerLock, lobbyLock;
	
	protected final SerializerReaderWriter serializer;
	
	public Client(SocketAddress bindAddress) throws SocketException {
		controller = new NetworkController(bindAddress);
		worldLock = new Object();
		playerLock = new Object();
		lobbyLock = new Object();
		controller.setOnRecieveMessage(this::process);
		serializer = new SerializerReaderWriter();
		registerClasses(serializer);
	}
	
	private static void registerClasses(SerializerReaderWriter s) { 
		//TODO remove the things from here that don't need to have separate id's, such as inventory or block grid which are built into other objects
		s.register(WorldObjectIdentifiers.BLOCK_GRID, BlockGrid.class, BlockGrid::new);
		s.register(WorldObjectIdentifiers.WORLD, ClientWorld.class, ClientWorld::new);
		s.register(WorldObjectIdentifiers.BLOCK_ITEM, BlockItem.class, BlockItem::new);
		s.register(WorldObjectIdentifiers.DIRT_BLOCK, DirtBlock.class, DirtBlock::new);
		s.register(WorldObjectIdentifiers.GRASS_BLOCK, GrassBlock.class, GrassBlock::new);
		s.register(WorldObjectIdentifiers.PLAYER_ENTITY, PlayerEntity.class, PlayerEntity::new);
		s.register(WorldObjectIdentifiers.INVENTORY, Inventory.class, Inventory::new);
		s.register(WorldObjectIdentifiers.ITEM_ENTITY, ItemEntity.class, ItemEntity::new);
		s.register(WorldObjectIdentifiers.PARTICLE_ENTITY, ParticleEntity.class, ParticleEntity::new);
	}
	
	public Client() throws SocketException, UnknownHostException {
		this(new InetSocketAddress(InetAddress.getLocalHost(), 0));
	}
	
	protected volatile int lastReceivedUnreliableMessageID = -1;
	
	protected final void process(SocketAddress sender, int messageID, byte[] data) {
		final short protocol = ByteUtil.getShort(data, 0);
		
		if(!Protocol.isReliable(protocol)) {
			if(messageID > lastReceivedUnreliableMessageID) {
				lastReceivedUnreliableMessageID = messageID;
			} else {
				return; //discard if not the latest unreliable message
			}
		}
		
		if(sender.equals(server)) {
			switch(protocol) {
				case Protocol.SERVER_CONNECT_ACKNOWLEDGMENT:
					processServerConnectAcknowledgement(data);
					break;
				case Protocol.SERVER_WORLD_HEAD:
					worldPackets = new byte[ByteUtil.getInteger(data, 2)][];
					break;
				case Protocol.SERVER_WORLD_DATA:
					processReceiveWorldData(data);
					break;
				case Protocol.CONSOLE_MESSAGE:
					System.out.println("[Server] " + new String(data, 2, data.length - 2, Protocol.CHARSET));
					break;
				case Protocol.SERVER_ENTITY_UPDATE:
					processGenericEntityUpdate(data);
					break;
				case Protocol.SERVER_ADD_ENTITY:
					processAddEntity(data);
					break;
				case Protocol.SERVER_REMOVE_ENTITY:
					processRemoveEntity(data);
					break;
				case Protocol.SERVER_CLIENT_DISCONNECT:
					processServerDisconnect(data);
					break;
				case Protocol.SERVER_PLAYER_ID:
					processReceivePlayerEntityID(data);
					break;
				case Protocol.SERVER_REMOVE_BLOCK:
					processServerRemoveBlock(data);
					break;
				
				default:
					System.err.println("Client received message of unknown protocol " + protocol);
			}
		}
	}
	
	private void processServerRemoveBlock(byte[] data) {
		world.getForeground().set(ByteUtil.getInteger(data, 2), ByteUtil.getInteger(data, 6), null);
	}

	public void sendBlockBreak(int x, int y) {
		byte[] packet = new byte[10];
		ByteUtil.putShort(packet, 0, Protocol.CLIENT_BREAK_BLOCK);
		ByteUtil.putInteger(packet, 2, x);
		ByteUtil.putInteger(packet, 6, y);
		send(packet);
	}
	
	public static enum PlayerAction {
		MOVE_LEFT,
		MOVE_RIGHT,
		MOVE_UP,
		MOVE_DOWN
	}
	
	public void sendPlayerAction(PlayerAction action, boolean enable) {
		if(isConnected()) {
			byte code = 0;
			switch(action) {
				case MOVE_LEFT:
					code = Protocol.PlayerAction.PLAYER_LEFT;
					break;
				case MOVE_RIGHT:
					code = Protocol.PlayerAction.PLAYER_RIGHT;
					break;
				case MOVE_UP:
					code = Protocol.PlayerAction.PLAYER_UP;
					break;
				case MOVE_DOWN:
					code = Protocol.PlayerAction.PLAYER_DOWN;
					break;
				default:
					throw new RuntimeException("unknown player action");
			}
			
			send(Client.buildPlayerAction(code, enable));
		} else {
			throw new RuntimeException("Client not connected to a server");
		}
	}
	
	private int nextReliableMessageID() {
		return reliableMessageID++;
	}
	
	private int nextUnreliableMessageID() {
		return unreliableMessageID++;
	}
	
	public void send(byte[] data) {
		if(isConnected()) {
			try {
				if(Protocol.isReliable(ByteUtil.getShort(data, 0))) {
					controller.sendReliable(server, nextReliableMessageID(), data, 10, 100);
				} else {
					controller.sendUnreliable(server, nextUnreliableMessageID(), data);
				}
			} catch(TimeoutException e) {
				disconnect(false);
			}
		} else {
			throw new ConnectionException("client is not connected to a server");
		}
	}
	
	public boolean isConnected() {
		return server != null && connected;
	}
	
	public SocketAddress getServer() {
		return server;
	}
	
	/**
	 * Waits on {@code lock} and returns {@code object} as soon as it has a value
	 * @param lock the object to wait to be notified by
	 * @param object the value to wait to be non-null
	 * @return the value of object
	 */
	protected <T extends Transportable> T receiveObject(Object lock, Supplier<T> object) {
		if(isConnected()) {
			synchronized(lock) {
				while(object.get() == null) {
					try {
						lock.wait();
					} catch(InterruptedException e) {
						continue;
					}
				}
				return object.get();
			}
		} else {
			throw new ConnectionException("client is not connected to a server");
		}	
	}
	
	public Lobby getLobby() {
		return lobby != null ? lobby : receiveObject(lobbyLock, () -> lobby);
	}
	
	public ClientWorld getWorld() {
		return world != null ? world : receiveObject(worldLock, () -> world);
	}
	
	public PlayerEntity getPlayer() {
		return player != null ? player : receiveObject(playerLock, () -> player);
	}
	
	public boolean connectTo(SocketAddress address, int timeout) {
		if(!isConnected()) {
			server = address;
			byte[] packet = new byte[2];
			ByteUtil.putShort(packet, 0, Protocol.CLIENT_CONNECT_REQUEST);
			try {
				controller.sendReliable(address, nextReliableMessageID(), packet, Math.max(1, timeout/100), Math.min(timeout, 100));
				synchronized(server) {
					server.wait();
				}
				return connected;
			} catch(TimeoutException e) {
				server = null;
				return false;
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}
		} else {
			disconnect(true);
			return connectTo(address, timeout);
		}
	
	}
	
	public void start() {
		controller.start();
	}
	
	public void stop() {
		disconnect(true);
		controller.stop();
	}
	
	public void disconnect(boolean notifyServer) {
		if(isConnected()) {
			try {
				if(notifyServer) {
					byte[] packet = new byte[2];
					ByteUtil.putShort(packet, 0, Protocol.CLIENT_DISCONNECT);
					send(packet);
				}
			} finally { //allow the client to be reused, reset all values TODO perhaps put all of this state in a seperate ConnectionState object?
				server = null;
				connected = false;
				unreliableMessageID = 0;
				reliableMessageID = 0;
				worldPackets = null;
				player = null;
				world = null;
			}
		}
	}
	
	private void processServerDisconnect(byte[] data) {
		disconnect(false);
		System.out.println("Disconnected from server: " + new String(data, 6, ByteUtil.getInteger(data, 2), Protocol.CHARSET));
	}
	
	private void processServerConnectAcknowledgement(byte[] data) {
		connected = ByteUtil.getBoolean(data, 2);
		synchronized(server) {
			server.notifyAll();
		}
	}
	
	private void processReceivePlayerEntityID(byte[] data) {
		int id = ByteUtil.getInteger(data, 2);
		while(player == null) { //loop infinitely until there is an entity with the correct ID
			Entity player = getWorld().find(id);
			if(player != null) {
				this.player = (PlayerEntity)player;
				synchronized(playerLock) {
					playerLock.notifyAll();
				}
			}
		}
	}
	
	private void processRemoveEntity(byte[] data) {
		getWorld().queueRemove(getWorld().find(ByteUtil.getInteger(data, 2)));
	}
	
	private void processAddEntity(byte[] data) {
		try {
			//waits until world is received and decompresses/deserializes/adds entity
			//will only be added to the world once the world is updated
			getWorld().add((Entity)ByteUtil.deserialize(ByteUtil.getBoolean(data, 2) ? ByteUtil.decompress(Arrays.copyOfRange(data, 3, data.length)) : Arrays.copyOfRange(data, 3, data.length)));
		} catch(ClassCastException | ReflectiveOperationException e) {
			System.err.println("Error while deserializing received entity");
		}
	}
	
	private void processGenericEntityUpdate(byte[] data) {
		if(world != null) {
			Entity e = world.find(ByteUtil.getInteger(data, 2)); //find the entity with the received entity id
			if(e != null) {
				float posX = ByteUtil.getFloat(data, 6);
				float posY = ByteUtil.getFloat(data, 10);
				//TODO causes some slightly weird glitches
				//dont update position on client if they are really similar
				if(Utility.distance(e.getPositionX(), e.getPositionY(), posX, posY) > 0.5f) {
					e.setPositionX(posX);
					e.setPositionY(posY);
				}
				e.setVelocityX(ByteUtil.getFloat(data, 14));
				e.setVelocityY(ByteUtil.getFloat(data, 18));
			}
		}
	}
	
	private final void processReceiveWorldData(byte[] data) {
		synchronized(worldPackets) {
			for(int i = 0; i < worldPackets.length; i++) {
				//find an empty slot to put the received data
				if(worldPackets[i] == null) {
					worldPackets[i] = data;
					
					//received final packet? create the world.
					if(i == worldPackets.length - 1) {
						world = (ClientWorld)reconstructWorld(worldPackets, ClientWorld.class, serializer);
						System.out.println("Received world data.");
						synchronized(worldLock) {
							worldLock.notifyAll(); //notify waitForWorldStart that the world has started
						}
						
						worldPackets = null;
					}
					break;
				}
			}
		}
	}

	public static final byte[] buildPlayerAction(byte action, boolean enable) {
		byte[] packet = new byte[4];
		ByteUtil.putShort(packet, 0, Protocol.CLIENT_PLAYER_ACTION);
		packet[2] = action;
		ByteUtil.putBoolean(packet, 3, enable);
		return packet;
	}

	public static <T extends World> World reconstructWorld(byte[][] data, Class<T> type, SerializerReaderWriter deserializer) {
		final int headerSize = 2; //WORLD_DATA protocol
		//create a sum of the number of bytes so that a single byte array can be allocated
		int bytes = 0;
		for(byte[] a : data) {
			bytes += (a.length - headerSize);
		}
		
		//the size of the world data (sum of all arrays without 2 byte headers)
		byte[] worldBytes = new byte[bytes];
		
		//copy the received packets into the final world data array
		int index = 0;
		for(byte[] array : data) {
			System.arraycopy(array, headerSize, worldBytes, index, array.length - headerSize);
			index += (array.length - headerSize);
		}
		
		try {
			return deserializer.deserialize(ByteUtil.decompress(worldBytes)); //TODO stalling here
			//return type.getConstructor(byte[].class).newInstance(ByteUtil.decompress(worldBytes));
		} catch(ReflectiveOperationException e) {
			e.printStackTrace();
			return null;
		}
	}
}
