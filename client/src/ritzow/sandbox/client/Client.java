package ritzow.sandbox.client;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;

/**
 * Represents a connection to a game server. Can only connect to a server once.
 * @author Solomon Ritzow
 *
 */
public final class Client {
	private final InetSocketAddress server;
	private final NetworkController controller;
	private volatile boolean connected;
	private volatile int unreliableMessageID, reliableMessageID, lastReceivedUnreliableMessageID = -1;
	
	/** The World object sent by the server **/
	private volatile World world;
	private byte[][] worldPackets;
	
	/** The Player object sent by the server for the client to control **/
	private volatile ClientPlayerEntity player;
	
	/** lock object for synchronizing server-initiated actions with the client **/
	private final Object worldLock, playerLock;
	private final SerializerReaderWriter serializer;
	private final Integrater integrater;
	
	/**
	 * Creates a client bound to the provided address
	 * @param bindAddress
	 * @throws SocketException
	 */
	public Client(InetSocketAddress bindAddress, InetSocketAddress serverAddress) throws SocketException {
		controller = new NetworkController(bindAddress, this::process);
		server = serverAddress;
		worldLock = new Object();
		playerLock = new Object();
		serializer = SerializationProvider.getProvider();
		integrater = new Integrater();
	}
	
	private void process(InetSocketAddress sender, int messageID, byte[] data) {
		if(!sender.equals(server))
			return;
		DataReader reader = new ByteArrayDataReader(data);
		short protocol = reader.readShort();

		if(!Protocol.isReliable(protocol)) {
			if(messageID > lastReceivedUnreliableMessageID) {
				lastReceivedUnreliableMessageID = messageID;
			} else {
				return; //discard if not the latest unreliable message
			}
		}
		
		try {
			if(integrater.running) {
				integrater.integrate(() -> onReceive(protocol, reader));
			} else {
				onReceive(protocol, reader);
			}
		} catch(RuntimeException e) {
			e.printStackTrace();
			disconnect(false);
		}
	}
	
	private void onReceive(short protocol, DataReader data) {
		switch(protocol) {
			case Protocol.CONSOLE_MESSAGE:
				int length = data.remaining();
				System.out.println("[Server Message] " + 
						new String(data.readBytes(data.remaining()), 0, length, Protocol.CHARSET));
				break;
			case Protocol.SERVER_CONNECT_ACKNOWLEDGMENT:
				processServerConnectAcknowledgement(data);
				break;
			case Protocol.SERVER_WORLD_HEAD:
				worldPackets = new byte[data.readInteger()][];
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
			case Protocol.SERVER_REMOVE_ENTITY: //TODO separate removing entity from world and getting rid of entity
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
	
	private static final class Integrater implements Runnable {
		private final Queue<Runnable> queue = new ConcurrentLinkedQueue<Runnable>();
		public boolean running;
		public void run() {
			running = true;
			while(!queue.isEmpty())
				queue.remove().run();
		}
		
		public void integrate(Runnable run) {
			queue.add(run);
		}
	}
	
	public Runnable onReceiveMessageTask() {
		return integrater;
	}
	
	private void processServerRemoveBlock(DataReader data) {
		world.getForeground().destroy(world, data.readInteger(), data.readInteger());
	}
	
	public boolean connect(int timeout) {
		if(!isConnected()) {
			controller.start();
			try {
				sendClientConnectRequest(server, timeout);
				synchronized(server) {
					server.wait();
				}
				if(!connected) {
					controller.stop();
				}
				return connected;
			} catch(TimeoutException e) {
				controller.stop();
				connected = false;
				return false;
			} catch (InterruptedException e) {
				throw new RuntimeException("client connect method should never be interrupted", e);
			}
		} else {
			throw new IllegalStateException("client already connected to connect to a server");
		}
	}
	
	public void send(byte[] data) {
		if(!connected)
			throw new IllegalStateException("client is not connected to a server");
		if(Protocol.isReliable(ByteUtil.getShort(data, 0))) {
			try{
				controller.sendReliable(server, nextReliableMessageID(), data, 10, 100);
			} catch(TimeoutException e) {
				disconnect(false);
			}
		} else {
			controller.sendUnreliable(server, nextUnreliableMessageID(), data);
		}
	}
	
	public boolean isConnected() {
		return connected;
	}
	
	public InetSocketAddress getServerAddress() {
		return server;
	}
	
	public void disconnect(boolean notifyServer) {
		if(!isConnected())
			throw new IllegalStateException("not connected to a server");
		if(notifyServer) {
			byte[] packet = new byte[2];
			ByteUtil.putShort(packet, 0, Protocol.CLIENT_DISCONNECT);
			send(packet);
		}
		connected = false;
		controller.stop();
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
	
	public World getWorld() {
		Utility.waitOnCondition(worldLock, () -> world != null);
		return world;
	}
	
	public ClientPlayerEntity getPlayer() {
		Utility.waitOnCondition(playerLock, () -> player != null);
		return player;
	}
	
	private void processServerDisconnect(DataReader data) {
		disconnect(false);
		int length = data.readInteger(); //unused length
		System.out.println("Disconnected from server: " + 
				new String(data.readBytes(data.remaining()), 0, length, Protocol.CHARSET));
	}
	
	private void processServerConnectAcknowledgement(DataReader data) {
		connected = data.readBoolean();
		synchronized(server) {
			server.notifyAll();
		}
	}
	
	private void processReceivePlayerEntityID(DataReader data) {
		int id = data.readInteger();
		while(player == null) { //loop infinitely until there is an entity with the correct ID
			for(Entity e : getWorld()) {
				if(e.getID() == id) {
					this.player = (ClientPlayerEntity)e;
					synchronized(playerLock) {
						playerLock.notifyAll();
					}
					return;
				}
			}
		}
	}
	
	private void processRemoveEntity(DataReader data) {
		int id = data.readInteger(); //ByteUtil.getInteger(data, 2);
		getWorld().removeIf(e -> e.getID() == id);
	}
	
	private void processAddEntity(DataReader data) {
		try {
			boolean compressed = data.readBoolean(); //ByteUtil.getBoolean(data, 2);
			byte[] entity = data.readBytes(data.remaining()); //Arrays.copyOfRange(data, 3, data.length);
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
		synchronized(worldPackets) {
			for(int i = 0; i < worldPackets.length; i++) {
				//find an empty slot to put the received data
				if(worldPackets[i] == null) {
					worldPackets[i] = data.readBytes(data.remaining());
					
					//deserialize the world after receiving the final packet
					if(i == worldPackets.length - 1) {
						world = reconstructWorld(worldPackets, serializer);
						world.setRemoveEntities(false);
						System.out.println("Received world data.");
						synchronized(worldLock) {
							worldLock.notifyAll();
						}
						worldPackets = null; //release the raw data to the garbage collector!
					}
					break;
				}
			}
		}
	}

	//TODO this should be generalized for anything spread accros packets
	private static World reconstructWorld(byte[][] data, SerializerReaderWriter deserializer) {
		final int headerSize = 0; //for WORLD_DATA protocol id
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
