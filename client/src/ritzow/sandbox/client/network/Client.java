package ritzow.sandbox.client.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
//import java.util.EnumMap;
//import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import ritzow.sandbox.client.util.SerializationProvider;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.data.ByteArrayDataReader;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.network.Protocol.PlayerAction;
import ritzow.sandbox.network.TimeoutException;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.PlayerEntity;

public class Client {
	private final DatagramChannel channel;
	private final BlockingQueue<SendPacket> sendQueue;
	private int receiveReliableID, receiveUnreliableID, sendUnreliableID; //only accessed by one thread
	private volatile int sendReliableID; //accessed by both threads
	private volatile boolean receivedByServer;
	private final Object connectLock, worldLock, playerLock, worldStateLock, reliableSendLock;
	private final ByteBuffer messageBuffer, responseBuffer;
	private volatile Status status;
	private volatile WorldState worldState;
	private volatile Executor runner;
	private volatile long pingNanos;
	
	//private final Map<SendChannel, BlockingQueue<SendPacket>> sendQueues;
	
	private static enum Status {
		NOT_CONNECTED,
		REJECTED,
		CONNECTED,
		DISCONNECTED
	}
	
//	private static enum SendChannel {
//		MOVEMENT,
//		ACTION
//	}
	
	private static class SendPacket {
		final byte[] data;
		final boolean reliable;
		
		public SendPacket(byte[] data, boolean reliable) {
			this.data = data;
			this.reliable = reliable;
		}
		
		public String toString() {
			return "SendPacket[reliable: " + reliable + " size:" + data.length + "]";
		}
	}
	
	private static final class WorldState {
		volatile World world;
		volatile ClientPlayerEntity player;
		volatile byte[] worldData;
		volatile int worldBytesRemaining;
	}
	
	public static final class ConnectionFailedException extends Exception {
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
	public static Client connect(InetSocketAddress bindAddress, InetSocketAddress serverAddress, Executor runner) 
			throws IOException, ConnectionFailedException {
		return new Client(bindAddress, serverAddress, runner);
	}
	
	private Client(InetSocketAddress bindAddress, InetSocketAddress serverAddress, Executor runner) throws ConnectionFailedException, IOException {
		this.runner = runner;
		this.status = Status.NOT_CONNECTED;
		this.channel = DatagramChannel.open(Utility.getProtocolFamily(bindAddress.getAddress())).bind(bindAddress).connect(serverAddress);
		messageBuffer = ByteBuffer.allocateDirect(Protocol.MAX_PACKET_SIZE);
		responseBuffer = ByteBuffer.allocateDirect(Protocol.HEADER_SIZE);
		sendQueue = new LinkedBlockingQueue<>();
		connectLock = new Object();
		worldLock = new Object();
		playerLock = new Object();
		worldStateLock = new Object();
		reliableSendLock = new Object();
//		sendQueues = new EnumMap<>(SendChannel.class);
//		for(SendChannel channel : SendChannel.values()) {
//			sendQueues.put(channel, new LinkedBlockingQueue<SendPacket>());
//		}
		new Thread(this::sender, "Packet Sender").start();
		new Thread(this::receiver, "Packet Receiver").start();
		connect();
	}
	
	private void connect() throws ConnectionFailedException {
		sendConnectRequest();
		//received ack on request packet, wait one second for response
		Utility.waitOnCondition(connectLock, 1000, () -> status != Status.NOT_CONNECTED);
		if(status == Status.REJECTED) {
			disconnectWithoutNotify();
			throw new ConnectionFailedException("connection rejected");
		} else if(status == Status.NOT_CONNECTED) {
			disconnectWithoutNotify();
			throw new ConnectionFailedException("no connect acknowledgement received");
		}
	}
	
	private void sendConnectRequest() {
		sendQueue.add(new SendPacket(Bytes.of(Protocol.TYPE_CLIENT_CONNECT_REQUEST), true));
	}
	
	public InetSocketAddress getServerAddress() { 
		try {
			return (InetSocketAddress)channel.getRemoteAddress();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void sender() {
		try {
			ByteBuffer sendBuffer = ByteBuffer.allocateDirect(Protocol.MAX_PACKET_SIZE);
			while(status == Status.NOT_CONNECTED || status == Status.CONNECTED) {
				SendPacket packet = sendQueue.take(); //TODO deal with blocking when disconnecting
				sendNext(sendBuffer, packet);
			}
		} catch (TimeoutException e) {
			//TODO handle timeout with server
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private void receiver() {
		try {
			while(status == Status.NOT_CONNECTED || status == Status.CONNECTED) {
				channel.receive(messageBuffer);
				processPacket(messageBuffer, responseBuffer);
			}
		} catch(IOException e) {
			//TODO handle?
		}
	}
	
	private static void setupSendBuffer(ByteBuffer sendBuffer, byte type, int id, byte[] data) {
		sendBuffer.put(type).putInt(id).put(data).flip();
	}
	
	private void sendNext(ByteBuffer sendBuffer, SendPacket packet) throws InterruptedException, IOException, TimeoutException {
		if(packet.reliable) {
			setupSendBuffer(sendBuffer, Protocol.RELIABLE_TYPE, sendReliableID, packet.data);
			int attempts = 0;
			long startTime = System.nanoTime();
			do {
				channel.write(sendBuffer);
				sendBuffer.rewind();
				synchronized(reliableSendLock) {
					reliableSendLock.wait(Protocol.RESEND_INTERVAL);
				}
				attempts++;
			} while(!receivedByServer && attempts < Protocol.RESEND_COUNT);
			
			if(receivedByServer) {
				pingNanos = Utility.nanosSince(startTime);
				receivedByServer = false;
				sendReliableID++;
			} else {
				throw new TimeoutException("reliable message " + packet + " , id: " + sendReliableID + " timed out.");
			}
		} else {
			setupSendBuffer(sendBuffer, Protocol.UNRELIABLE_TYPE, sendUnreliableID++, packet.data);
			channel.write(sendBuffer);
		}
		sendBuffer.clear();
	}
	
//	public void receive() throws IOException {
//		while(channel.read(messageBuffer) > 0) {
//			processPacket(messageBuffer, responseBuffer);
//		}
//	}
	
	private void processPacket(ByteBuffer buffer, ByteBuffer sendBuffer) throws IOException {
		buffer.flip(); //flip to set limit and prepare to read packet data
		if(buffer.limit() >= Protocol.HEADER_SIZE) {
			byte type = buffer.get(); //type of message (RESPONSE, RELIABLE, UNRELIABLE)
			int messageID = buffer.getInt(); //received ID or messageID for ack.
			switch(type) {
			case Protocol.RESPONSE_TYPE:
				if(sendReliableID == messageID) {
					receivedByServer = true;
					Utility.notify(reliableSendLock);
				} break; //else drop the response
			case Protocol.RELIABLE_TYPE:
				if(messageID == receiveReliableID) {
					//if the message is the next one, process it and update last message
					sendResponse(sendBuffer, messageID);
					receiveReliableID++;
					process(buffer.position(Protocol.HEADER_SIZE).slice());
				} else if(messageID < receiveReliableID) { //message already received
					sendResponse(sendBuffer, messageID);
				} break; //else: message received too early
			case Protocol.UNRELIABLE_TYPE:
				if(messageID >= receiveUnreliableID) {
					receiveUnreliableID = messageID + 1;
					process(buffer.position(Protocol.HEADER_SIZE).slice());
				} break; //else: message is outdated
			}
		}
		buffer.clear(); //clear to prepare for next receive
	}
	
	private void sendResponse(ByteBuffer sendBuffer, int receivedMessageID) throws IOException {
		channel.write(sendBuffer.put(Protocol.RESPONSE_TYPE).putInt(receivedMessageID).flip());
		sendBuffer.clear();
	}
	
	private void process(ByteBuffer buffer) {
		byte[] data = new byte[buffer.remaining()];
		buffer.get(data);
		if(status == Status.CONNECTED || status == Status.NOT_CONNECTED) {
			runner.execute(() -> onReceive(new ByteArrayDataReader(data)));
		}
	}
	
	private void onReceive(DataReader data) {
		short type = data.readShort();
		if(type == Protocol.TYPE_SERVER_CONNECT_ACKNOWLEDGMENT) {
			processServerConnectAcknowledgement(data);
		} else if(status == Status.CONNECTED) {
			switch(type) {
			case Protocol.TYPE_CONSOLE_MESSAGE:
				System.out.println("[Server Message] " + new String(data.readBytes(data.remaining()), Protocol.CHARSET));
				break;
			case Protocol.TYPE_SERVER_WORLD_DATA:
				processReceiveWorldData(data);
				break;
			case Protocol.TYPE_SERVER_ENTITY_UPDATE:
				processUpdateEntity(data);
				break;
			case Protocol.TYPE_SERVER_ADD_ENTITY:
				processAddEntity(data);
				break;
			case Protocol.TYPE_SERVER_REMOVE_ENTITY:
				processRemoveEntity(data);
				break;
			case Protocol.TYPE_SERVER_CLIENT_DISCONNECT:
				processServerDisconnect(data);
				break;
			case Protocol.TYPE_SERVER_PLAYER_ID:
				processReceivePlayerEntityID(data);
				break;
			case Protocol.TYPE_SERVER_REMOVE_BLOCK:
				processServerRemoveBlock(data);
				break;
			case Protocol.TYPE_SERVER_PING:
				break;
			case Protocol.TYPE_SERVER_PLAYER_ACTION:
				processServerPlayerAction(data);
				break;
			default:
				throw new IllegalArgumentException("Client received message of unknown protocol " + type);
			}
		} else {
			throw new IllegalStateException("Received irrelevant message");
		}
	}
	
	private void processServerConnectAcknowledgement(DataReader data) {
		byte response = data.readByte();
		switch(response) {
		case Protocol.CONNECT_STATUS_REJECTED:
			status = Status.REJECTED;
			break;
		case Protocol.CONNECT_STATUS_WORLD:
			status = Status.CONNECTED;
			var state = new WorldState();
			int remaining = data.readInteger();
			state.worldBytesRemaining = remaining;
			state.worldData = new byte[remaining];
			this.worldState = state;
			Utility.notify(worldStateLock);
			break;
		case Protocol.CONNECT_STATUS_LOBBY:
			throw new UnsupportedOperationException("CONNECT_STATUS_LOBBY not supported");
		default:
			throw new RuntimeException("unknown connect ack type " + response);
		}
		
		Utility.notify(connectLock);
	}
	
	/** Redirect all received message processing to the provided TaskQueue **/
	public void setExecutor(Executor processor) {
		this.runner = processor;
	}
	
	public InetSocketAddress getAddress() {
		try {
			return (InetSocketAddress)channel.getLocalAddress();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public long getPing() {
		return pingNanos;
	}
	
	public boolean isConnected() {
		return status == Status.CONNECTED;
	}
	
	public void disconnectNotifyServer() {
		try {
			checkConnected();
			status = Status.DISCONNECTED;
			sendReliable(Bytes.of(Protocol.TYPE_CLIENT_DISCONNECT));
		} finally {
			try {
				channel.close(); //TODO wait for sending and receiving threads to finish
			} catch (IOException e) {
				e.printStackTrace();
			}
			Utility.notify(worldStateLock);
		}
	}
	
	private void disconnectWithoutNotify() {
		status = Status.DISCONNECTED;
		try {
			channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Utility.notify(worldStateLock);
	}
	
	public World getWorld() {
		checkConnected();
		Utility.waitOnCondition(worldLock, () -> worldState.world != null);
		return worldState.world;
	}
	
	public ClientPlayerEntity getPlayer() {
		checkConnected();
		Utility.waitOnCondition(playerLock, () -> worldState.player != null);
		return worldState.player;
	}
	
	private void checkConnected() {
		if(status != Status.CONNECTED)
			throw new IllegalStateException("client is not connected to a server");
	}
	
	private void sendReliable(byte[] data) {
		sendQueue.add(new SendPacket(data.clone(), true));
	}
	
	@SuppressWarnings("unused")
	private void sendUnreliable(byte[] data) {
		sendQueue.add(new SendPacket(data.clone(), false));
	}
	
	public void sendBombThrow(float angle) {
		checkConnected();
		byte[] packet = new byte[2 + 4];
		Bytes.putShort(packet, 0, Protocol.TYPE_CLIENT_BOMB_THROW);
		Bytes.putFloat(packet, 2, angle);
		sendReliable(packet);
	}
	
	public void sendBlockBreak(int x, int y) {
		checkConnected();
		byte[] packet = new byte[10];
		Bytes.putShort(packet, 0, Protocol.TYPE_CLIENT_BREAK_BLOCK);
		Bytes.putInteger(packet, 2, x);
		Bytes.putInteger(packet, 6, y);
		sendReliable(packet);
	}
	
	public void sendPlayerAction(PlayerAction action, boolean enable) {
		checkConnected();
		byte[] packet = new byte[4];
		Bytes.putShort(packet, 0, Protocol.TYPE_CLIENT_PLAYER_ACTION);
		packet[2] = action.getCode();
		Bytes.putBoolean(packet, 3, enable);
		sendReliable(packet);
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Entity> T getEntityFromID(int ID) {
		for(Entity e : getWorld()) { //block until world is received so no NPEs happen
			if(e.getID() == ID) {
				return (T)e;
			}
		}
		throw new IllegalStateException("No entity with ID " + ID + " exists"); //TODO dont throw exception under certain circumstances?
	}
	
	private void processServerPlayerAction(DataReader data) {
		PlayerEntity e = getEntityFromID(data.readInteger());
		e.processAction(PlayerAction.forCode(data.readByte()), data.readBoolean());
	}
	
	private void processServerRemoveBlock(DataReader data) {
		worldState.world.getForeground().destroy(worldState.world, data.readInteger(), data.readInteger());
	}
	
	private void processServerDisconnect(DataReader data) {
		disconnectWithoutNotify();
		int length = data.readInteger();
		System.out.println("Disconnected from server: " + 
				new String(data.readBytes(data.remaining()), 0, length, Protocol.CHARSET));
	}
	
	private void processReceivePlayerEntityID(DataReader data) {
		int id = data.readInteger();
		worldState.player = getEntityFromID(id);
		Utility.notify(playerLock);
	}
	
	private void processRemoveEntity(DataReader data) {
		int id = data.readInteger();
		worldState.world.removeIf(e -> e.getID() == id);
	}
	
	private static <T> T deserialize(boolean compress, byte[] data) {
		return SerializationProvider.getProvider().deserialize(compress ? Bytes.decompress(data) : data);
	}
	
	private void processAddEntity(DataReader data) {
		worldState.world.add(deserialize(data.readBoolean(), data.readBytes(data.remaining())));
	}
	
	private void processUpdateEntity(DataReader data) {
		int id = data.readInteger();
		World world = worldState.world;
		for(Entity e : world) {
			if(e.getID() == id) {
				e.setPositionX(data.readFloat());
				e.setPositionY(data.readFloat());
				e.setVelocityX(data.readFloat());
				e.setVelocityY(data.readFloat());
				return;
			}
		}
	}
	
	private void processReceiveWorldData(DataReader data) {
		WorldState state = this.worldState;
		if(state.worldData == null)
			throw new IllegalStateException("world head packet has not been received");
		synchronized(state.worldData) {
			int dataSize = data.remaining();
			Bytes.copy(data.readBytes(dataSize), state.worldData, state.worldData.length - state.worldBytesRemaining);
			boolean receivedAll = (state.worldBytesRemaining -= dataSize) == 0;
			//System.out.println(Utility.formatSize(state.worldBytesRemaining) + " remaining of " + Utility.formatSize(state.worldData.length) + " of world data.");
			if(receivedAll) {
				buildWorld();
			}
		}
	}
	
	private void buildWorld() {
		System.out.print("Building world... ");
		long start = System.nanoTime();
		worldState.world = deserialize(Protocol.COMPRESS_WORLD_DATA, worldState.worldData);
		System.out.println("took " + Utility.formatTime(Utility.nanosSince(start)) + ".");
		worldState.worldData = null; //release the raw data to the garbage collector
		sendReliable(Bytes.of(Protocol.TYPE_CLIENT_WORLD_BUILT));
		Utility.notify(worldLock);
	}
}
