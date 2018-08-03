package ritzow.sandbox.client.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import ritzow.sandbox.client.util.SerializationProvider;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.network.Protocol.PlayerAction;
import ritzow.sandbox.network.TimeoutException;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.PlayerEntity;

//currently has queued blocking receive processing, queued blocking message sending
public class Client {

	private static final long CONNECT_TIMEOUT_MILLIS = 2000;

	//sender/receiver state
	private final DatagramChannel channel;
	private final BlockingQueue<SendPacket> sendQueue;
	private int receiveReliableID, receiveUnreliableID, sendUnreliableID; //only accessed by one thread
	private volatile int sendReliableID; //accessed by both threads
	private volatile boolean receivedByServer;
	private final Thread sendThread, receiveThread;

	//game state
	private final Object connectLock, worldLock, playerLock, reliableSendLock;
	private volatile Executor runner;
	private volatile Status status;
	private volatile WorldState worldState;
	private volatile long pingNanos;
	private volatile boolean finishedDisconnect;

//	private final Map<SendChannel, BlockingQueue<SendPacket>> sendQueues;
//
//	public void receive() throws IOException {
//		while(channel.read(messageBuffer) > 0) {
//			processPacket(messageBuffer, responseBuffer);
//		}
//	}
//
//	private static enum SendChannel {
//		MOVEMENT,
//		ACTION
//	}
//
//	sendQueues = new EnumMap<>(SendChannel.class);
//	for(SendChannel channel : SendChannel.values()) {
//		sendQueues.put(channel, new LinkedBlockingQueue<SendPacket>());
//	}

	private static enum Status {
		NOT_CONNECTED(true),
		REJECTED(false),
		CONNECTED(true),
		DISCONNECTING(true),
		DISCONNECTED(false);

		final boolean transfer;

		Status(boolean transfer) {
			this.transfer = transfer;
		}

	}

	private static final class WorldState {
		volatile World world;
		volatile ClientPlayerEntity player;
		volatile byte[] worldData;
		volatile int worldBytesRemaining;
	}

	public static final class ConnectionFailedException extends Exception {
		ConnectionFailedException(String message) {
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
		sendQueue = new LinkedBlockingQueue<>();
		connectLock = new Object();
		worldLock = new Object();
		playerLock = new Object();
		reliableSendLock = new Object();
		(sendThread = new Thread(this::sender, "Packet Sender")).start();;
		(receiveThread = new Thread(this::receiver, "Packet Receiver")).start();
		connect();
	}

	private void connect() throws ConnectionFailedException, IOException {
		sendReliable(Bytes.of(Protocol.TYPE_CLIENT_CONNECT_REQUEST));
		//received ack on request packet, wait one second for response
		//TODO stop sender and receiver threads (share code with disconnect methods?)
		Utility.waitOnCondition(connectLock, CONNECT_TIMEOUT_MILLIS, () -> status != Status.NOT_CONNECTED);
		if(status == Status.REJECTED) {
			channel.close();
			throw new ConnectionFailedException("connection rejected");
		} else if(status == Status.NOT_CONNECTED) {
			channel.close();
			throw new ConnectionFailedException("no connect acknowledgement received");
		}
	}

	public InetSocketAddress getLocalAddress() {
		try {
			return (InetSocketAddress)channel.getLocalAddress();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public InetSocketAddress getServerAddress() {
		try {
			return (InetSocketAddress)channel.getRemoteAddress();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void onReceive(ByteBuffer data) {
		try {
			short type = data.getShort();
			if(type == Protocol.TYPE_SERVER_CONNECT_ACKNOWLEDGMENT) {
				processServerConnectAcknowledgement(data);
			} else if(status == Status.CONNECTED) {
				switch(type) {
				case Protocol.TYPE_CONSOLE_MESSAGE:
					System.out.println("[Server Message] " + new String(getRemainingBytes(data), Protocol.CHARSET));
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
				throw new IllegalStateException("Received non-TYPE_SERVER_CONNECT_ACKNOWLEDGMENT message before connecting to server");
			}
		} catch(IOException e) {
			//TODO shutdown client on IOException
			e.printStackTrace();
		}
	}

	private void processServerConnectAcknowledgement(ByteBuffer data) {
		byte response = data.get();
		switch(response) {
		case Protocol.CONNECT_STATUS_REJECTED:
			status = Status.REJECTED;
			break;
		case Protocol.CONNECT_STATUS_WORLD:
			status = Status.CONNECTED;
			var state = new WorldState();
			int remaining = data.getInt();
			state.worldBytesRemaining = remaining;
			state.worldData = new byte[remaining];
			this.worldState = state;
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

	/**
	 * @return the round trip time for the last
	 * reliable message sent to the server, in nanoseconds.
	 */
	public long getPing() {
		return pingNanos;
	}

	/**
	 * @return true if the client is connected to the server and can transmit game related messages.
	 */
	public boolean isConnected() {
		return status == Status.CONNECTED;
	}

	/**
	 * Throws an {@link IllegalStateException} if the client is not in a normal
	 * connected state where game related messages can be sent and received.
	 */
	private void checkConnected() {
		if(status != Status.CONNECTED)
			throw new IllegalStateException("client is not connected to a server");
	}

	/**
	 * Notifies the server, disconnects, and stops the client. May block
	 * while disconnect is occuring.
	 * @throws IOException if an internal I/O socket error occurs
	 * @throws InterruptedException if the client is interrupted while disconnecting
	 */
	public void disconnect() throws IOException, InterruptedException {
		checkConnected();
		status = Status.DISCONNECTING;
		sendReliable(Bytes.of(Protocol.TYPE_CLIENT_DISCONNECT)); //TODO wait for response message to be received by server (is there even a way to do this?)
		finishedDisconnect = true;
		status = Status.DISCONNECTED;
		sendThread.join(); //TODO interrupt sender/receiver.
		receiveThread.join();
		channel.close();
	}

	/**
	 * Disconnects and stops the client without notifying the server (occurs without delay/waiting).
	 * @throws IOException
	 */
	private void disconnectWithoutNotify() throws IOException {
		checkConnected();
		finishedDisconnect = true;
		status = Status.DISCONNECTED;
		channel.close();
	}

	/**
	 * Returns the World object sent by the server, blocking if necessary until a World is available.
	 * @return the current world instance
	 * @throws InterruptedException if interrupted while waiting to receive the world
	 */
	public World getWorld() throws InterruptedException {
		checkConnected();
		if(worldState == null)
			throw new IllegalStateException("No world to get, not currently in a world.");
		Utility.waitOnCondition(worldLock, () -> worldState.world != null);
		return worldState.world;
	}

	/**
	 * Returns the player associated with this client, sent by the server.
	 * Blocks until this client's player is received and identified.
	 * @return the ClientPlayerEntity associated with this client, provided by the server
	 * @throws InterruptedException if interrupted while waiting to receive the player
	 */
	public ClientPlayerEntity getPlayer() throws InterruptedException {
		checkConnected();
		Utility.waitOnCondition(playerLock, () -> worldState.player != null);
		return worldState.player;
	}

	/** Queues a reliable message **/
	private void sendReliable(byte[] data) {
		sendQueue.add(new SendPacket(data.clone(), true));
	}

	/** Queues an unreliable messager **/
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

	public void sendPlayerAction(PlayerAction action) {
		checkConnected();
		byte[] packet = new byte[3];
		Bytes.putShort(packet, 0, Protocol.TYPE_CLIENT_PLAYER_ACTION);
		packet[2] = action.getCode();
		sendReliable(packet);
	}

	private static byte[] getRemainingBytes(ByteBuffer buffer) {
		byte[] data = new byte[buffer.remaining()];
		buffer.get(data);
		return data;
	}

	@SuppressWarnings("unchecked")
	private <T extends Entity> T getEntityFromID(int ID) {
		for(Entity e : worldState.world) { //block until world is received so no NPEs happen
			if(e.getID() == ID) {
				return (T)e;
			}
		}
		throw new IllegalStateException("No entity with ID " + ID + " exists");
	}

	private void processServerPlayerAction(ByteBuffer data) {
		PlayerEntity e = getEntityFromID(data.getInt());
		e.processAction(PlayerAction.forCode(data.get()));
	}

	private void processServerRemoveBlock(ByteBuffer data) {
		World world = worldState.world;
		world.getForeground().destroy(world, data.getInt(), data.getInt());
	}

	private void processServerDisconnect(ByteBuffer data) throws IOException {
		disconnectWithoutNotify();
		int length = data.getInt();
		byte[] array = new byte[length];
		data.get(array);
		System.out.println("Disconnected from server: " + new String(array, Protocol.CHARSET));
	}

	private void processReceivePlayerEntityID(ByteBuffer data) {
		int id = data.getInt();
		worldState.player = getEntityFromID(id);
		Utility.notify(playerLock);
	}

	private void processRemoveEntity(ByteBuffer data) {
		int id = data.getInt();
		worldState.world.removeIf(e -> e.getID() == id);
	}

	private static <T> T deserialize(boolean compress, byte[] data) {
		return SerializationProvider.getProvider().deserialize(compress ? Bytes.decompress(data) : data);
	}

	private void processAddEntity(ByteBuffer data) {
		worldState.world.add(deserialize(data.get() == 1 ? true : false, getRemainingBytes(data)));
	}

	private void processUpdateEntity(ByteBuffer data) {
		int entityID = data.getInt();
		for(Entity e : worldState.world) {
			if(e.getID() == entityID) {
				e.setPositionX(data.getFloat());
				e.setPositionY(data.getFloat());
				e.setVelocityX(data.getFloat());
				e.setVelocityY(data.getFloat());
				return;
			}
		}
	}

	private void processReceiveWorldData(ByteBuffer data) {
		WorldState state = this.worldState;
		if(state.worldData == null)
			throw new IllegalStateException("world head packet has not been received");
		synchronized(state.worldData) {
			int dataSize = data.remaining();
			data.get(state.worldData, state.worldData.length - state.worldBytesRemaining, dataSize);
			boolean receivedAll = (state.worldBytesRemaining -= dataSize) == 0;
			if(receivedAll) buildWorld();
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

	private static class SendPacket {
		final byte[] data;
		final boolean reliable;

		public SendPacket(byte[] data, boolean reliable) {
			this.data = data;
			this.reliable = reliable;
		}

		@Override
		public String toString() {
			return "SendPacket[" + (reliable ? "reliable" : "unreliable") + ", size:" + data.length + "]";
		}
	}

	private void sender() {
		try {
			ByteBuffer sendBuffer = ByteBuffer.allocateDirect(Protocol.MAX_PACKET_SIZE);
			while(!finishedDisconnect) {
				SendPacket packet = sendQueue.take(); //TODO deal with blocking when disconnecting by interrupting
				sendNext(sendBuffer, packet);
			}
		} catch (TimeoutException | InterruptedException | IOException e) {
			e.printStackTrace(); //TODO handle TimeoutException and InterruptedException, close client on IOException
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
			} while(!receivedByServer && attempts < Protocol.RESEND_COUNT && status.transfer);

			Utility.notify(packet); //if the sender of the packet wants to be notified when the message is sent

			if(receivedByServer) {
				pingNanos = Utility.nanosSince(startTime);
				receivedByServer = false;
				sendReliableID++;
			} else if(status.transfer) {
				throw new TimeoutException(packet + " with ID " + sendReliableID + " timed out.");
			} //else return, no longer transferring
		} else {
			setupSendBuffer(sendBuffer, Protocol.UNRELIABLE_TYPE, sendUnreliableID++, packet.data);
			channel.write(sendBuffer);
		}
		sendBuffer.clear();
	}

	private void process(ByteBuffer buffer) {
		ByteBuffer clone = ByteBuffer.allocate(buffer.capacity()); //temporary until receiving is single-threaded
		clone.put(buffer);
		clone.position(0);
		runner.execute(() -> onReceive(clone));
	}

	private void receiver() {
		try {
			var receiveBuffer = ByteBuffer.allocateDirect(Protocol.MAX_PACKET_SIZE);
			var responseBuffer = ByteBuffer.allocateDirect(Protocol.HEADER_SIZE);
			while(!finishedDisconnect) {
				channel.receive(receiveBuffer);
				processReceived(receiveBuffer, responseBuffer);
			}
		} catch(IOException e) {
			//TODO shutdown client on IOException
			e.printStackTrace();
		}
	}

	private void processReceived(ByteBuffer buffer, ByteBuffer sendBuffer) throws IOException {
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

	private void sendResponse(ByteBuffer responseBuffer, int receivedMessageID) throws IOException {
		channel.write(responseBuffer.put(Protocol.RESPONSE_TYPE).putInt(receivedMessageID).flip());
		responseBuffer.clear();
	}
}
