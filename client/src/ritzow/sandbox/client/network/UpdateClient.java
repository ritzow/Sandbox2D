package ritzow.sandbox.client.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiConsumer;
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
public class UpdateClient implements GameTalker {

	//sender/receiver state
	private final DatagramChannel channel;
	private final Queue<SendPacket> sendQueue;
	private final Map<SendPacket, Runnable> messageSentActions;
	private int receiveReliableID, receiveUnreliableID, sendReliableID, sendUnreliableID;
	
	//reliable message send state
	private int sendAttempts;
	private long sendTime;

	//game state
	private Status status;
	private WorldState worldState;
	private long ping; //rount trip time in nanoseconds

	public static enum Status {
		CONNECTING(true),
		CONNECTED(true),
		DISCONNECTING(true),
		DISCONNECTED(false),
		REJECTED(true);
		
		boolean requiresCommunication;
		
		Status(boolean requiresCommunication) {
			this.requiresCommunication = requiresCommunication;
		}
	}

	private static final class WorldState {
		World world;
		ClientPlayerEntity player;
		byte[] worldData;
		int worldBytesRemaining;
	}

	public static final class ConnectionFailedException extends Exception {
		ConnectionFailedException(String message) {
			super(message);
		}
	}
	
	private final Object[] events = new Object[4];
	
	public static final class ClientEvent<T> {
		public static final ClientEvent<Runnable> CONNECT_ACCEPTED = new ClientEvent<>();
		public static final ClientEvent<Runnable> CONNECT_REJECTED = new ClientEvent<>();
		public static final ClientEvent<Runnable> DISCONNECTED = new ClientEvent<>();
		public static final ClientEvent<BiConsumer<World, ClientPlayerEntity>> WORLD_JOIN = new ClientEvent<>();
		private static byte count;
		int index;
		
		ClientEvent() {
			this.index = count++;
		}
	}
	
	public <T> void setEventListener(ClientEvent<T> eventType, T action) {
		events[eventType.index] = action;
	}
	
	@SuppressWarnings("unchecked")
	private <T> T getAction(ClientEvent<T> eventType) {
		return (T)events[eventType.index];
	}
	
	/**
	 * Creates a client bound to the provided address
	 * @param bindAddress the local address to bind to.
	 * @throws IOException if an internal I/O error occurrs
	 * @throws SocketException if the local address could not be bound to.
	 */
	public static UpdateClient startConnection(InetSocketAddress bindAddress, InetSocketAddress serverAddress) throws IOException {
		return new UpdateClient(bindAddress, serverAddress);
	}
	
	private UpdateClient(InetSocketAddress bindAddress, InetSocketAddress serverAddress) throws IOException {
		this.channel = DatagramChannel.open(Utility.getProtocolFamily(bindAddress.getAddress())).bind(bindAddress).connect(serverAddress);
		this.channel.configureBlocking(false);
		this.status = Status.CONNECTING;
		this.sendQueue = new ArrayDeque<>();
		this.messageSentActions = new HashMap<>();
		sendReliable(Bytes.of(Protocol.TYPE_CLIENT_CONNECT_REQUEST));
	}
	
	public Status getStatus() {
		return status;
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
		short type = data.getShort();
		if(type == Protocol.TYPE_SERVER_CONNECT_ACKNOWLEDGMENT) {
			processServerConnectAcknowledgement(data);
		} else if(status != Status.CONNECTING) {
			switch(type) {
				case Protocol.TYPE_CONSOLE_MESSAGE -> processServerConsoleMessage(data);
				case Protocol.TYPE_SERVER_WORLD_DATA -> processReceiveWorldData(data);
				case Protocol.TYPE_SERVER_ENTITY_UPDATE -> processUpdateEntity(data);
				case Protocol.TYPE_SERVER_ADD_ENTITY -> processAddEntity(data);
				case Protocol.TYPE_SERVER_REMOVE_ENTITY -> processRemoveEntity(data);
				case Protocol.TYPE_SERVER_CLIENT_DISCONNECT -> processServerDisconnect(data);
				case Protocol.TYPE_SERVER_PLAYER_ID -> processReceivePlayerEntityID(data);
				case Protocol.TYPE_SERVER_REMOVE_BLOCK -> processServerRemoveBlock(data);
				case Protocol.TYPE_SERVER_PLAYER_ACTION -> processServerPlayerAction(data);
				case Protocol.TYPE_SERVER_PING -> {}
				default -> throw new IllegalArgumentException("Client received message of unknown protocol " + type);
			}
		} else {
			throw new IllegalStateException("Received non-TYPE_SERVER_CONNECT_ACKNOWLEDGMENT message before connecting to server");
		}
	}
	
	/**
	 * @return the round trip time for the last
	 * reliable message sent to the server, in nanoseconds.
	 */
	public long getPing() {
		return ping;
	}

	/**
	 * Notifies the server, disconnects, and stops the client. May block
	 * while disconnect is occuring.
	 * @throws IOException if an internal I/O socket error occurs
	 * @throws InterruptedException if the client is interrupted while disconnecting
	 */
	public void startDisconnect() {
		status = Status.DISCONNECTING;
		sendReliable(Bytes.of(Protocol.TYPE_CLIENT_DISCONNECT), () -> {
			status = Status.DISCONNECTED;
			var action = getAction(ClientEvent.DISCONNECTED);
			if(action != null) action.run();
		});
	}

	/**
	 * Returns the World object sent by the server, blocking if necessary until a World is available.
	 * @return the current world instance
	 * @throws InterruptedException if interrupted while waiting to receive the world
	 */
	public World getWorld() throws InterruptedException {
		if(worldState == null)
			return null;
		return worldState.world;
	}

	/**
	 * Returns the player associated with this client, sent by the server.
	 * Blocks until this client's player is received and identified.
	 * @return the ClientPlayerEntity associated with this client, provided by the server
	 * @throws InterruptedException if interrupted while waiting to receive the player
	 */
	public ClientPlayerEntity getPlayer() throws InterruptedException {
		if(worldState == null)
			return null;
		return worldState.player;
	}

	/** Queues a reliable message **/
	private void sendReliable(byte[] data) {
		sendQueue.add(new SendPacket(data.clone(), true));
	}
	
	/** Queues a reliable message, and runs an action after the message is received by the server **/
	private void sendReliable(byte[] data, Runnable action) {
		SendPacket packet = new SendPacket(data.clone(), true);
		sendQueue.add(packet);
		messageSentActions.put(packet, action);
	}

	/** Queues an unreliable messager **/
	@SuppressWarnings("unused")
	private void sendUnreliable(byte[] data) {
		sendQueue.add(new SendPacket(data.clone(), false));
	}

	public void sendBombThrow(float angle) {
		byte[] packet = new byte[2 + 4];
		Bytes.putShort(packet, 0, Protocol.TYPE_CLIENT_BOMB_THROW);
		Bytes.putFloat(packet, 2, angle);
		sendReliable(packet);
	}

	public void sendBlockBreak(int x, int y) {
		byte[] packet = new byte[10];
		Bytes.putShort(packet, 0, Protocol.TYPE_CLIENT_BREAK_BLOCK);
		Bytes.putInteger(packet, 2, x);
		Bytes.putInteger(packet, 6, y);
		sendReliable(packet);
	}

	public void sendPlayerAction(PlayerAction action) {
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
	
	private static void processServerConsoleMessage(ByteBuffer data) {
		System.out.println("[Server Message] " + new String(getRemainingBytes(data), Protocol.CHARSET));
	}

	private void processServerConnectAcknowledgement(ByteBuffer data) {
		byte response = data.get();
		switch(response) {
			case Protocol.CONNECT_STATUS_REJECTED -> {
				status = Status.REJECTED;
				var action = getAction(ClientEvent.CONNECT_REJECTED);
				if(action != null) action.run();
			}
			case Protocol.CONNECT_STATUS_WORLD -> {
				var state = new WorldState();
				int remaining = data.getInt();
				state.worldBytesRemaining = remaining;
				state.worldData = new byte[remaining];
				this.worldState = state;
				status = Status.CONNECTED;
				var action = getAction(ClientEvent.CONNECT_ACCEPTED);
				if(action != null) action.run();
			}
			case Protocol.CONNECT_STATUS_LOBBY -> 
				throw new UnsupportedOperationException("CONNECT_STATUS_LOBBY not supported");
			default -> throw new UnsupportedOperationException("unknown connect ack type " + response);
		}
	}

	/**
	 * Abruptly close the client without notifying a connected server
	 * @throws IOException if an exception occurs when closing the networking socket
	 */
	public void close() throws IOException {
		channel.close();
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

	private void processServerDisconnect(ByteBuffer data) {
		status = Status.DISCONNECTED;
		int length = data.getInt();
		byte[] array = new byte[length];
		data.get(array);
		System.out.println("Disconnected from server: " + new String(array, Protocol.CHARSET));
		var action = getAction(ClientEvent.DISCONNECTED);
		if(action != null) action.run();
	}

	private void processReceivePlayerEntityID(ByteBuffer data) {
		int id = data.getInt();
		worldState.player = getEntityFromID(id);
		var action = getAction(ClientEvent.WORLD_JOIN);
		if(action != null) action.accept(worldState.world, worldState.player);
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
		int dataSize = data.remaining();
		data.get(state.worldData, state.worldData.length - state.worldBytesRemaining, dataSize);
		boolean receivedAll = (state.worldBytesRemaining -= dataSize) == 0;
		if(receivedAll) buildWorld();
	}

	private void buildWorld() {
		System.out.print("Building world... ");
		long start = System.nanoTime();
		worldState.world = deserialize(Protocol.COMPRESS_WORLD_DATA, worldState.worldData);
		System.out.println("took " + Utility.formatTime(Utility.nanosSince(start)) + ".");
		worldState.worldData = null; //release the raw data to the garbage collector
		sendReliable(Bytes.of(Protocol.TYPE_CLIENT_WORLD_BUILT));
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
	
	private final ByteBuffer 
		receiveBuffer = ByteBuffer.allocateDirect(Protocol.MAX_PACKET_SIZE), //received messages
		responseBuffer = ByteBuffer.allocateDirect(Protocol.HEADER_SIZE), //message responses
		sendBuffer = ByteBuffer.allocateDirect(Protocol.MAX_PACKET_SIZE); //message sending
	
	public void update() throws TimeoutException, IOException {
		//receive incoming packets
		//already connected to server so no need to check SocketAddress
		while(channel.isOpen() && channel.receive(receiveBuffer) != null) {
			processReceived(receiveBuffer, responseBuffer);
		}
		
		while(channel.isOpen() && !sendQueue.isEmpty()) {
			SendPacket packet = sendQueue.peek();
			if(packet.reliable) {
				if(sendAttempts == 0) {
					setupSendBuffer(sendBuffer, Protocol.RELIABLE_TYPE, sendReliableID, packet.data);
					sendTime = System.nanoTime();
					sendReliableInternal(sendBuffer);
				} else if(Utility.sendIntervalElapsed(sendTime, sendAttempts)) {
					if(sendAttempts < Protocol.RESEND_COUNT) {
						sendReliableInternal(sendBuffer);
					} else {
						throw new TimeoutException("failed to send");	
					}
				} break; //need to be able to receive a response
			} else {
				setupSendBuffer(sendBuffer, Protocol.UNRELIABLE_TYPE, sendUnreliableID++, packet.data);
				channel.write(sendBuffer);
				sendBuffer.clear();
				sendQueue.poll();
			}
		}
	}

	private static void setupSendBuffer(ByteBuffer sendBuffer, byte type, int id, byte[] data) {
		sendBuffer.put(type).putInt(id).put(data).flip();
	}
	
	private void sendReliableInternal(ByteBuffer message) throws IOException {
		channel.write(message);
		sendBuffer.rewind();
		sendAttempts++;
	}
	
	private void processReceived(ByteBuffer buffer, ByteBuffer responseBuffer) throws IOException {
		buffer.flip(); //flip to set limit and prepare to read packet data
		if(buffer.limit() >= Protocol.HEADER_SIZE) {
			byte type = buffer.get(); //type of message (RESPONSE, RELIABLE, UNRELIABLE)
			int messageID = buffer.getInt(); //received ID or messageID for ack.
			switch(type) {
				case Protocol.RESPONSE_TYPE -> {
					if(sendReliableID == messageID) {
						ping = Utility.nanosSince(sendTime);
						sendAttempts = 0;
						sendReliableID++;
						sendBuffer.clear();
						var action = messageSentActions.get(sendQueue.poll());
						if(action != null)
							action.run();
					} //else drop the response
				}
				
				case Protocol.RELIABLE_TYPE -> {
					if(messageID == receiveReliableID) {
						//if the message is the next one, process it and update last message
						sendResponse(responseBuffer, messageID);
						receiveReliableID++;
						process(buffer);
					} else if(messageID < receiveReliableID) { //message already received
						sendResponse(responseBuffer, messageID);
					} //else: message received too early	
				}
				
				case Protocol.UNRELIABLE_TYPE -> {
					if(messageID >= receiveUnreliableID) {
						receiveUnreliableID = messageID + 1;
						process(buffer);
					} //else: message is outdated	
				}
			}
		}
		buffer.clear(); //clear to prepare for next receive
	}
	
	private void process(ByteBuffer buffer) {
		onReceive(buffer.position(Protocol.HEADER_SIZE));
	}

	private void sendResponse(ByteBuffer responseBuffer, int receivedMessageID) throws IOException {
		channel.write(responseBuffer.put(Protocol.RESPONSE_TYPE).putInt(receivedMessageID).flip());
		responseBuffer.clear();
	}
}
