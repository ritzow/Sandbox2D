package ritzow.sandbox.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import ritzow.sandbox.client.util.SerializationProvider;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.data.ByteArrayDataReader;
import ritzow.sandbox.data.ByteUtil;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.network.NetworkController;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.network.Protocol.PlayerAction;
import ritzow.sandbox.network.TimeoutException;
import ritzow.sandbox.util.TaskQueue;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.PlayerEntity;

public class Client {
	private final InetSocketAddress serverAddress;
	private final NetworkController network;
	private final Object worldLock, playerLock, connectionLock;
	private volatile byte connectionStatus;
	private volatile ConnectionState state;
	private Runnable disconnectAction;
	private TaskQueue taskQueue;
	
	private static final byte STATUS_NOT_CONNECTED = 0, STATUS_REJECTED = 1, STATUS_CONNECTED = 2;
	
	private static final class ConnectionState {
		volatile World world;
		volatile ClientPlayerEntity player;
		volatile byte[] worldData;
		volatile int worldBytesRemaining;
	}
	
	public static final class ConnectionFailedException extends RuntimeException {
		public ConnectionFailedException(String message) {
			super(message);
		}
	}
	
	/**
	 * Creates a client bound to the provided address
	 * @param bindAddress the local address to bind to.
	 * @throws IOException if an internal I/O error occurrs
	 * @throws SocketException if the local address could not be bound to.
	 * @throws ConnectionFailedException if the client could not connect to the specified server
	 */
	public static Client open(InetSocketAddress bindAddress, InetSocketAddress serverAddress) 
			throws IOException, ConnectionFailedException {
		return new Client(bindAddress, serverAddress);
	}
	
	private Client(InetSocketAddress bindAddress, InetSocketAddress serverAddress) throws IOException, ConnectionFailedException {
		network = new NetworkController(bindAddress, this::process);
		worldLock = new Object();
		playerLock = new Object();
		connectionLock = new Object();
		this.serverAddress = serverAddress;
		connect();
	}
	
	private void connect() {
		network.start();
		synchronized(serverAddress) {
			try {
				byte[] packet = new byte[2];
				ByteUtil.putShort(packet, 0, Protocol.CLIENT_CONNECT_REQUEST);
				network.sendReliable(serverAddress, packet, 10, 100);
			} catch(TimeoutException e) { //if client receives no ack, disconnect and throw
				disconnect(false);
				throw new ConnectionFailedException("request timed out");
			}
			//received ack on request packet, wait one second for response
			Utility.waitOnCondition(serverAddress, 1000, () -> connectionStatus != STATUS_NOT_CONNECTED);
		}
		if(connectionStatus == STATUS_CONNECTED) {
			state = new ConnectionState();
			Utility.notify(connectionLock); //to ensure that nothing tries to access state before it is created
		} else if(connectionStatus == STATUS_REJECTED) {
			disconnect(false);
			throw new ConnectionFailedException("connection rejected");
		} else {
			disconnect(false);
			throw new ConnectionFailedException("request timed out");
		}
	}
	
	private void process(InetSocketAddress sender, byte[] data) {
		try {
			if(!sender.equals(serverAddress))
				return;
			DataReader reader = new ByteArrayDataReader(data);
			short type = reader.readShort();
			if(taskQueue == null)
				onReceive(type, reader);
			else taskQueue.add(() -> onReceive(type, reader));
		} catch(RuntimeException e) {
			e.printStackTrace();
			disconnect(false);
		}
	}
	
	private void onReceive(short type, DataReader data) {
		if(type == Protocol.SERVER_CONNECT_ACKNOWLEDGMENT) {
			processServerConnectAcknowledgement(data);
			return;
		}
		
		Utility.waitOnCondition(connectionLock, () -> state != null);
		switch(type) {
			case Protocol.CONSOLE_MESSAGE:
				int length = data.remaining();
				System.out.println("[Server Message] " + new String(data.readBytes(data.remaining()), 0, length, Protocol.CHARSET));
				break;
			case Protocol.SERVER_WORLD_HEAD:
				state.worldBytesRemaining = data.readInteger();
				state.worldData = new byte[state.worldBytesRemaining];
				break;
			case Protocol.SERVER_WORLD_DATA:
				processReceiveWorldData(data);
				break;
			case Protocol.SERVER_ENTITY_UPDATE:
				processGenericEntityUpdate(data);
				break;
			case Protocol.SERVER_ADD_ENTITY: //TODO separate receiving entity and adding to world
				processAddEntity(data);
				break;
			case Protocol.SERVER_REMOVE_ENTITY: //TODO separate removing entity from world and deleting entity
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
			case Protocol.PING:
				break;
			case Protocol.SERVER_PLAYER_ACTION:
				PlayerEntity e = getEntityFromID(data.readInteger());
				e.processAction(PlayerAction.forCode(data.readByte()), data.readBoolean());
				break;
			default:
				throw new IllegalArgumentException("Client received message of unknown protocol " + type);
		}
	}
	
	public void setOnDisconnect(Runnable task) {
		disconnectAction = task;
	}
	
	public InetSocketAddress getServerAddress() {
		return serverAddress;
	}
	
	public boolean isConnected() {
		return connectionStatus == STATUS_CONNECTED;
	}
	
	public void disconnect() {
		checkConnected();
		disconnect(true);
	}
	
	/** Redirect all received message processing to the provided TaskQueue **/
	public void setTaskQueue(TaskQueue processor) {
		taskQueue = processor;
	}
	
	private void disconnect(boolean notifyServer) {
		connectionStatus = STATUS_NOT_CONNECTED;
		if(notifyServer) {
			byte[] packet = new byte[2];
			ByteUtil.putShort(packet, 0, Protocol.CLIENT_DISCONNECT);
			network.sendReliable(serverAddress, packet, 10, 100);
		}
		network.stop();
		if(disconnectAction != null)
			disconnectAction.run();
		state = null;
	}
	
	public World getWorld() {
		checkConnected();
		Utility.waitOnCondition(worldLock, () -> state.world != null);
		return state.world;
	}
	
	public ClientPlayerEntity getPlayer() {
		checkConnected();
		Utility.waitOnCondition(playerLock, () -> state.player != null);
		return state.player;
	}
	
	private void checkConnected() {
		if(!isConnected())
			throw new IllegalStateException("client is not connected to a server");
	}
	
	private void sendReliable(byte[] data) {
		try{
			network.sendReliable(serverAddress, data, 10, 100);
		} catch(TimeoutException e) {
			disconnect(false);
		}		
	}
	
	@SuppressWarnings("unused")
	private void sendUnreliable(byte[] data) {
		checkConnected();
		network.sendUnreliable(serverAddress, data);
	}

	public void sendBlockBreak(int x, int y) {
		byte[] packet = new byte[10];
		ByteUtil.putShort(packet, 0, Protocol.CLIENT_BREAK_BLOCK);
		ByteUtil.putInteger(packet, 2, x);
		ByteUtil.putInteger(packet, 6, y);
		sendReliable(packet);
	}
	
	public void sendPlayerAction(PlayerAction action, boolean enable) {
		if(isConnected()) {
			byte[] packet = new byte[4];
			ByteUtil.putShort(packet, 0, Protocol.CLIENT_PLAYER_ACTION);
			packet[2] = action.getCode();
			ByteUtil.putBoolean(packet, 3, enable);
			sendReliable(packet);
		} else {
			throw new IllegalStateException("Client not connected to a server");
		}
	}
	
	//Game Functionality and Processing
	
	private <T extends Entity> T getEntityFromID(int ID) {
		for(Entity e : getWorld()) {
			if(e.getID() == ID) {
				return (T)e;
			}
		}
		throw new IllegalStateException("no entity with ID " + ID + " exists");
	}
	
	private void processServerRemoveBlock(DataReader data) {
		state.world.getForeground().destroy(state.world, data.readInteger(), data.readInteger());
	}
	
	private void processServerDisconnect(DataReader data) {
		disconnect(false);
		int length = data.readInteger();
		System.out.println("Disconnected from server: " + 
				new String(data.readBytes(data.remaining()), 0, length, Protocol.CHARSET));
	}
	
	private void processServerConnectAcknowledgement(DataReader data) {
		connectionStatus = data.readBoolean() ? STATUS_CONNECTED : STATUS_REJECTED;
		Utility.notify(serverAddress);
	}
	
	private void processReceivePlayerEntityID(DataReader data) {
		int id = data.readInteger();
		state.player = getEntityFromID(id);
		Utility.notify(playerLock);
	}
	
	private void processRemoveEntity(DataReader data) {
		int id = data.readInteger();
		state.world.removeIf(e -> e.getID() == id);
	}
	
	private void processAddEntity(DataReader data) {
		try {
			boolean compressed = data.readBoolean();
			byte[] entity = data.readBytes(data.remaining());
			Entity e = SerializationProvider.getProvider().deserialize(compressed ? ByteUtil.decompress(entity) : entity);
			state.world.forEach(o -> {
				if(o.getID() == e.getID())
					throw new IllegalStateException("cannot have two entities with the same ID");
			});
			state.world.add(e);
		} catch(ClassCastException e) {
			System.err.println("Error while deserializing received entity");
		}
	}
	
	private void processGenericEntityUpdate(DataReader data) {
		int id = data.readInteger();
		for(Entity e : state.world) {
			if(e.getID() == id) {
				e.setPositionX(data.readFloat());
				e.setPositionY(data.readFloat());
				e.setVelocityX(data.readFloat());
				e.setVelocityY(data.readFloat());
				return;
			}
		}
		throw new RuntimeException("no entity with id " + id + " found to update");
	}
	
	private void processReceiveWorldData(DataReader data) {
		ConnectionState state = this.state;
		if(state.worldData == null)
			throw new IllegalStateException("world head packet has not been received");
		synchronized(state.worldData) {
			int remaining = data.remaining();
			ByteUtil.copy(data.readBytes(remaining), state.worldData, state.worldData.length - state.worldBytesRemaining);
			if((state.worldBytesRemaining -= remaining) == 0) {
				state.world = SerializationProvider.getProvider().deserialize(ByteUtil.decompress(state.worldData));
				state.worldData = null; //release the raw data to the garbage collector!
				Utility.notify(worldLock);
			}
		}
	}
}
