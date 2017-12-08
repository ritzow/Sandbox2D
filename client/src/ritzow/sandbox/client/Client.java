package ritzow.sandbox.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import ritzow.sandbox.client.util.SerializationProvider;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.data.ByteUtil;
import ritzow.sandbox.data.SerializerReaderWriter;
import ritzow.sandbox.network.NetworkController;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.network.Protocol.PlayerAction;
import ritzow.sandbox.network.TimeoutException;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;

/**
 * Represents a connection to a game server. Can only connect to a server once.
 * @author Solomon Ritzow
 *
 */
public final class Client {
	
	/** The server's address **/
	private volatile SocketAddress server;
	
	/** I/O logic **/
	private final NetworkController controller;
	
	/** if the server has been successfully connected to **/
	private volatile boolean connected;
	
	/** messageID **/
	private volatile int unreliableMessageID, reliableMessageID, lastReceivedUnreliableMessageID = -1;
	
	/** The World object sent by the server **/
	private volatile World world;
	private byte[][] worldPackets;
	
	/** The Player object sent by the server for the client to control **/
	private volatile ClientPlayerEntity player;
	
	/** lock object for synchronizing server-initiated actions with the client **/
	private final Object worldLock, playerLock;
	private final SerializerReaderWriter serializer;
	
	public Client(SocketAddress bindAddress) throws SocketException {
		controller = new NetworkController(bindAddress);
		worldLock = new Object();
		playerLock = new Object();
		controller.setOnRecieveMessage(this::process);
		serializer = SerializationProvider.getProvider();
	}
	
	public Client() throws SocketException, UnknownHostException {
		this(new InetSocketAddress(InetAddress.getLocalHost(), 0));
	}
	
	private void onReceive(final short protocol, int messageID, byte[] data) {
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
				System.out.println("[Server Message] " + new String(data, 2, data.length - 2, Protocol.CHARSET));
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
				throw new IllegalArgumentException("Client received message of unknown protocol " + protocol);
		}
	}
	
	private void process(SocketAddress sender, int messageID, byte[] data) {
		final short protocol = ByteUtil.getShort(data, 0);
		
		if(!Protocol.isReliable(protocol)) {
			if(messageID > lastReceivedUnreliableMessageID) {
				lastReceivedUnreliableMessageID = messageID;
			} else {
				return; //discard if not the latest unreliable message
			}
		}
		
		if(sender.equals(server)) {
			try {
				onReceive(protocol, messageID, data);
			} catch(IllegalArgumentException e) {
				System.err.println(e.getMessage());
			}
		}
	}
	
	private void processServerRemoveBlock(byte[] data) {
		world.getForeground().destroy(world, ByteUtil.getInteger(data, 2), ByteUtil.getInteger(data, 6));
	}
	
	public boolean connectTo(SocketAddress address, int timeout) {
		if(!isConnected()) {
			controller.start();
			server = address;
			try {
				sendClientConnectRequest(address, timeout);
				synchronized(server) {
					server.wait();
				}
				if(!connected) {
					controller.stop();
					server = null;	
				}
				return connected;
			} catch(TimeoutException e) {
				controller.stop();
				server = null;
				return false;
			} catch (InterruptedException e) {
				throw new RuntimeException("client connect method should never be interrupted", e);
			}
		} else {
			throw new IllegalStateException("client already connected to connect to a server");
		}
	}
	
	private void sendClientConnectRequest(SocketAddress server, int timeout) {
		byte[] packet = new byte[2];
		ByteUtil.putShort(packet, 0, Protocol.CLIENT_CONNECT_REQUEST);
		controller.sendReliable(server, nextReliableMessageID(), packet, Math.max(1, timeout/100), Math.min(timeout, 100));
	}

	public void sendBlockBreak(int x, int y) {
		byte[] packet = new byte[10];
		ByteUtil.putShort(packet, 0, Protocol.CLIENT_BREAK_BLOCK);
		ByteUtil.putInteger(packet, 2, x);
		ByteUtil.putInteger(packet, 6, y);
		send(packet);
	}
	
	public void sendPlayerAction(PlayerAction action, boolean enable) {
		if(isConnected()) {
			byte[] packet = new byte[4];
			ByteUtil.putShort(packet, 0, Protocol.CLIENT_PLAYER_ACTION);
			packet[2] = action.getCode();
			ByteUtil.putBoolean(packet, 3, enable);
			send(packet);
		} else {
			throw new IllegalStateException("Client not connected to a server");
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
			throw new IllegalStateException("client is not connected to a server");
		}
	}
	
	public boolean isConnected() {
		return server != null && connected;
	}
	
	public SocketAddress getServer() {
		return server;
	}
	
	public World getWorld() {
		Utility.waitOnCondition(worldLock, () -> world != null);
		return world;
	}
	
	public ClientPlayerEntity getPlayer() {
		Utility.waitOnCondition(playerLock, () -> player != null);
		return player;
	}
	
	public void disconnect(boolean notifyServer) {
		if(isConnected()) {
			if(notifyServer) {
				byte[] packet = new byte[2];
				ByteUtil.putShort(packet, 0, Protocol.CLIENT_DISCONNECT);
				send(packet);
			}
			controller.stop();
		} else {
			throw new IllegalStateException("not connected to a server");
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
			getWorld().forEach(e -> {
				if(e.getID() == id) {
					this.player = (ClientPlayerEntity)e;
					synchronized(playerLock) {
						playerLock.notifyAll();
					}
					return;
				}
			});
		}
	}
	
	private void processRemoveEntity(byte[] data) {
		int id = ByteUtil.getInteger(data, 2);
		getWorld().removeIf(e -> e.getID() == id);
	}
	
	private void processAddEntity(byte[] data) {
		try {
			boolean compressed = ByteUtil.getBoolean(data, 2);
			byte[] entity = Arrays.copyOfRange(data, 3, data.length);
			Entity e = serializer.deserialize(compressed ? ByteUtil.decompress(entity) : entity);
			world.forEach(o -> {
				if(o.getID() == e.getID())
					throw new IllegalStateException("cannot have two entities with the same ID");
			});
			getWorld().add(e);
		} catch(ClassCastException e) {
			System.err.println("Error while deserializing received entity");
		}
	}
	
	private void processGenericEntityUpdate(byte[] data) {
		int id = ByteUtil.getInteger(data, 2);
		for (Entity e : world) {
			if(e.getID() == id) {
				e.setPositionX((e.getPositionX() + ByteUtil.getFloat(data, 6))/2);
				e.setPositionY((e.getPositionY() + ByteUtil.getFloat(data, 10))/2);
				e.setVelocityX(ByteUtil.getFloat(data, 14));
				e.setVelocityY(ByteUtil.getFloat(data, 18));
				return;
			}
		}
		System.err.println("no entity with id " + id + " found to update");
	}
	
	private void processReceiveWorldData(byte[] data) {
		synchronized(worldPackets) {
			for(int i = 0; i < worldPackets.length; i++) {
				//find an empty slot to put the received data
				if(worldPackets[i] == null) {
					worldPackets[i] = data;
					
					//received final packet? create the world.
					if(i == worldPackets.length - 1) {
						world = reconstructWorld(worldPackets, serializer);
						world.setRemoveEntities(false);
						System.out.println("Received world data.");
						synchronized(worldLock) {
							worldLock.notifyAll();
						}
						worldPackets = null;
					}
					break;
				}
			}
		}
	}

	private static World reconstructWorld(byte[][] data, SerializerReaderWriter deserializer) {
		final int headerSize = 2; //for WORLD_DATA protocol id
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
		
		return deserializer.deserialize(ByteUtil.decompress(worldBytes));
	}
}
