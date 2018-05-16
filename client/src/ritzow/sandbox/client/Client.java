package ritzow.sandbox.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import ritzow.sandbox.client.util.SerializationProvider;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.data.ByteArrayDataReader;
import ritzow.sandbox.data.ByteUtil;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.data.SerializerReaderWriter;
import ritzow.sandbox.network.NetworkController;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.network.Protocol.PlayerAction;
import ritzow.sandbox.network.TimeoutException;
import ritzow.sandbox.util.TaskQueue;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;

/**
 * Represents a connection to a game server. Can only connect to a server once.
 * @author Solomon Ritzow
 *
 */
public class Client {
	private final InetSocketAddress server;
	private final NetworkController network;
	private final SerializerReaderWriter serializer;
	private final Integrator integrator;
	private final ExecutorService workers;
	private final Object worldLock, playerLock, connectionLock;
	private volatile byte connectionStatus;
	private volatile ConnectionState state;
	private Runnable disconnectAction;
	
	private static final byte STATUS_NOT_CONNECTED = 0, STATUS_REJECTED = 1, STATUS_CONNECTED = 2;
	
	private static final class Integrator extends TaskQueue {
		private boolean running;
		
		public void run() {
			running = true;
			super.run();
		}
		
		public boolean isRunning() {
			return running;
		}
	}
	
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
		server = serverAddress;
		worldLock = new Object();
		playerLock = new Object();
		connectionLock = new Object();
		serializer = SerializationProvider.getProvider();
		integrator = new Integrator();
		workers = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "Message Processor"));
		if(!connect()) {
			throw new ConnectionFailedException(connectionStatus == STATUS_REJECTED ?
					"connection rejected" : "connection timed out");
		}
	}
	
	private boolean connect() {
		if(isConnected())
			throw new IllegalStateException("client already connected to a server");
		network.start();
		synchronized(server) {
			try {
				byte[] packet = new byte[2];
				ByteUtil.putShort(packet, 0, Protocol.CLIENT_CONNECT_REQUEST);
				network.sendReliable(server, packet, 10, 100);
			} catch(TimeoutException e) {
				disconnect(false);
				return false;
			}
			Utility.waitOnCondition(server, 1000, () -> connectionStatus != STATUS_NOT_CONNECTED);
		}
		if(connectionStatus == STATUS_CONNECTED) {
			state = new ConnectionState();
			Utility.notify(connectionLock); //to ensure that nothing tries to access state before it is created
		} else {
			disconnect(false);
		}
		return isConnected();
	}
	
	private void process(InetSocketAddress sender, int messageID, byte[] data) {
		workers.execute(() -> {
			if(!sender.equals(server))
				return;
			DataReader reader = new ByteArrayDataReader(data);
			short protocol = reader.readShort();
			try {
				if(integrator.isRunning()) {
					integrator.add(() -> onReceive(protocol, reader)); //TODO improve this code, too much spaghetti!
				} else {
					onReceive(protocol, reader);
				}
			} catch(RuntimeException e) {
				e.printStackTrace();
				disconnect(false);
			}
		});
	}
	
	private void onReceive(short protocol, DataReader data) {
		switch(protocol) {
			case Protocol.CONSOLE_MESSAGE:
				int length = data.remaining();
				System.out.println("[Server Message] " + new String(data.readBytes(data.remaining()), 0, length, Protocol.CHARSET));
				break;
			case Protocol.SERVER_CONNECT_ACKNOWLEDGMENT:
				processServerConnectAcknowledgement(data);
				break;
			case Protocol.SERVER_WORLD_HEAD:
				state().worldBytesRemaining = data.readInteger();
				state().worldData = new byte[state.worldBytesRemaining];
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
			default:
				throw new IllegalArgumentException("Client received message of unknown protocol " + protocol);
		}
	}
	
	public void setOnDisconnect(Runnable task) {
		disconnectAction = task;
	}
	
	public Runnable onReceiveMessageTask() {
		return integrator;
	}
	
	private void processServerRemoveBlock(DataReader data) {
		state().world.getForeground().destroy(state().world, data.readInteger(), data.readInteger());
	}
	
	private ConnectionState state() {
		Utility.waitOnCondition(connectionLock, () -> state != null);
		return state;
	}
	
	public boolean isConnected() {
		return connectionStatus == STATUS_CONNECTED;
	}
	
	public InetSocketAddress getServerAddress() {
		return server;
	}
	
	public void disconnect() {
		if(isConnected())
			disconnect(true);
	}
	
	private void disconnect(boolean notifyServer) {
		try {
			if(notifyServer) {
				checkConnected();
				byte[] packet = new byte[2];
				ByteUtil.putShort(packet, 0, Protocol.CLIENT_DISCONNECT);
				sendReliable(packet);
			}
			network.stop();
			workers.shutdown();
			workers.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			if(disconnectAction != null)
				disconnectAction.run();
			state = null;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void sendReliable(byte[] data) {
		try{
			network.sendReliable(server, data, 10, 100);
		} catch(TimeoutException e) {
			disconnect(false);
		}		
	}
	
	@SuppressWarnings("unused")
	private void sendUnreliable(byte[] data) {
		checkConnected();
		network.sendUnreliable(server, data);
	}
	
	private void checkConnected() {
		if(!isConnected())
			throw new IllegalStateException("client is not connected to a server");
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
	
	private void processServerDisconnect(DataReader data) {
		disconnect(false);
		int length = data.readInteger();
		System.out.println("Disconnected from server: " + 
				new String(data.readBytes(data.remaining()), 0, length, Protocol.CHARSET));
	}
	
	private void processServerConnectAcknowledgement(DataReader data) {
		connectionStatus = data.readBoolean() ? STATUS_CONNECTED : STATUS_REJECTED;
		Utility.notify(server);
	}
	
	private void processReceivePlayerEntityID(DataReader data) {
		int id = data.readInteger();
		for(Entity e : getWorld()) {
			if(e.getID() == id) {
				state.player = (ClientPlayerEntity)e;
				Utility.notify(playerLock);
				return;
			}
		}
		throw new IllegalStateException("no entity with ID " + id + " exists");
	}
	
	private void processRemoveEntity(DataReader data) {
		int id = data.readInteger();
		getWorld().removeIf(e -> e.getID() == id);
	}
	
	private void processAddEntity(DataReader data) {
		try {
			boolean compressed = data.readBoolean();
			byte[] entity = data.readBytes(data.remaining());
			Entity e = serializer.deserialize(compressed ? ByteUtil.decompress(entity) : entity);
			state().world.forEach(o -> {
				if(o.getID() == e.getID())
					throw new IllegalStateException("cannot have two entities with the same ID");
			});
			getWorld().add(e);
		} catch(ClassCastException e) {
			System.err.println("Error while deserializing received entity");
		}
	}
	
	private void processGenericEntityUpdate(DataReader data) {
		int id = data.readInteger();
		for (Entity e : getWorld()) {
			if(e.getID() == id) {
				e.setPositionX(data.readFloat());
				e.setPositionY(data.readFloat());
				e.setVelocityX(data.readFloat());
				e.setVelocityY(data.readFloat());
				return;
			}
		}
		System.err.println("no entity with id " + id + " found to update");
	}
	
	private void processReceiveWorldData(DataReader data) {
		ConnectionState state = state();
		if(state.worldData == null)
			throw new IllegalStateException("world head packet has not been received");
		synchronized(state.worldData) {
			int remaining = data.remaining();
			ByteUtil.copy(data.readBytes(remaining), state.worldData, state.worldData.length - state.worldBytesRemaining);
			if((state.worldBytesRemaining -= remaining) == 0) {
				state.world = serializer.deserialize(ByteUtil.decompress(state.worldData));
				state.worldData = null; //release the raw data to the garbage collector!
				Utility.notify(worldLock);
			}
		}
	}
}
