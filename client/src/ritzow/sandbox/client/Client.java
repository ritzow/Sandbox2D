package ritzow.sandbox.client;

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
	private final Integrater integrater;
	private final ExecutorService workers;
	private Runnable disconnectAction;
	private volatile byte connectedStatus;
	
	private volatile World world;
	private byte[] worldData;
	private int worldBytesRemaining;
	private volatile ClientPlayerEntity player;
	private final Object worldLock, playerLock;
	
	private static final byte STATUS_NOT_CONNECTED = 0, STATUS_REJECTED = 1, STATUS_CONNECTED = 2;
	
	private static final class Integrater extends TaskQueue {
		public boolean running;
		public void run() {
			running = true;
			super.run();
		}
	}
	
	/**
	 * Creates a client bound to the provided address
	 * @param bindAddress
	 * @throws SocketException
	 */
	public Client(InetSocketAddress bindAddress, InetSocketAddress serverAddress) throws SocketException {
		network = new NetworkController(bindAddress, this::process);
		server = serverAddress;
		worldLock = new Object();
		playerLock = new Object();
		serializer = SerializationProvider.getProvider();
		integrater = new Integrater();
		workers = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "Message Processor"));
	}
	
	private void process(InetSocketAddress sender, int messageID, byte[] data) {
		workers.execute(() -> {
			if(!sender.equals(server))
				return;
			DataReader reader = new ByteArrayDataReader(data);
			short protocol = reader.readShort();
			try {
				if(integrater.running) {
					integrater.add(() -> onReceive(protocol, reader)); //TODO improve this code, too much spaghetti!
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
				worldBytesRemaining = data.readInteger();
				worldData = new byte[worldBytesRemaining];
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
	
	public void onDisconnect(Runnable task) {
		disconnectAction = task;
	}
	
	public Runnable onReceiveMessageTask() {
		return integrater;
	}
	
	private void processServerRemoveBlock(DataReader data) {
		world.getForeground().destroy(world, data.readInteger(), data.readInteger());
	}
	
	public boolean connect() {
		if(isConnected())
			throw new IllegalStateException("client already connected to a server");
		start();
		synchronized(server) {
			try {
				byte[] packet = new byte[2];
				ByteUtil.putShort(packet, 0, Protocol.CLIENT_CONNECT_REQUEST);
				network.sendReliable(server, packet, 10, 100);
			} catch(TimeoutException e) {
				stop();
				return false;
			}
			Utility.waitOnCondition(server, 1000, () -> connectedStatus != STATUS_NOT_CONNECTED);
		}
		if(connectedStatus != STATUS_CONNECTED)
			stop();
		return isConnected();
	}
	
	public boolean isConnected() {
		return connectedStatus == STATUS_CONNECTED;
	}
	
	public InetSocketAddress getServerAddress() {
		return server;
	}
	
	public void disconnect() {
		disconnect(true);
	}
	
	private void disconnect(boolean notifyServer) {
		checkConnected();
		if(notifyServer) {
			byte[] packet = new byte[2];
			ByteUtil.putShort(packet, 0, Protocol.CLIENT_DISCONNECT);
			sendReliable(packet);
		}
		stop();
		disconnectAction.run();
	}
	
	private void start() {
		network.start();
	}
	
	private void stop() {
		network.stop();
		try {
			workers.shutdown();
			workers.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
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
		Utility.waitOnCondition(worldLock, () -> world != null);
		return world;
	}
	
	public ClientPlayerEntity getPlayer() {
		checkConnected();
		Utility.waitOnCondition(playerLock, () -> player != null);
		return player;
	}
	
	private void processServerDisconnect(DataReader data) {
		disconnect(false);
		int length = data.readInteger();
		System.out.println("Disconnected from server: " + 
				new String(data.readBytes(data.remaining()), 0, length, Protocol.CHARSET));
	}
	
	private void processServerConnectAcknowledgement(DataReader data) {
		connectedStatus = data.readBoolean() ? STATUS_CONNECTED : STATUS_REJECTED;
		Utility.notify(server);
	}
	
	private void processReceivePlayerEntityID(DataReader data) {
		int id = data.readInteger();
		while(player == null) { //loop infinitely until there is an entity with the correct ID
			for(Entity e : getWorld()) {
				if(e.getID() == id) {
					this.player = (ClientPlayerEntity)e;
					Utility.notify(playerLock);
					return;
				}
			}
		}
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
			world.forEach(o -> {
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
		if(worldData == null)
			throw new IllegalStateException("world head packet has not been received");
		synchronized(worldData) {
			int remaining = data.remaining();
			ByteUtil.copy(data.readBytes(remaining), worldData, worldData.length - worldBytesRemaining);
			if((worldBytesRemaining -= remaining) == 0) {
				world = serializer.deserialize(ByteUtil.decompress(worldData));
				worldData = null; //release the raw data to the garbage collector!
				Utility.notify(worldLock);
			}
		}
	}
}
