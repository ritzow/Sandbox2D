package ritzow.sandbox.server.network;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Predicate;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Transportable;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.server.SerializationProvider;
import ritzow.sandbox.server.world.entity.ServerBombEntity;
import ritzow.sandbox.server.world.entity.ServerPlayerEntity;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.block.DirtBlock;
import ritzow.sandbox.world.block.GrassBlock;
import ritzow.sandbox.world.entity.BombEntity;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.entity.PlayerEntity;
import ritzow.sandbox.world.item.BlockItem;

import static ritzow.sandbox.network.Protocol.*;

/** The server manages connected game clients, sends game updates, 
 * receives client input, and broadcasts information for clients. */
public class Server {
	private static final long NETWORK_SEND_INTERVAL_NANOSECONDS = Utility.millisToNanos(200);
	private static final float BOMB_THROW_VELOCITY = Utility.convertPerSecondToPerNano(25);

	private final DatagramChannel channel;
	private final ByteBuffer receiveBuffer, responseBuffer, sendBuffer;
	private final Map<InetSocketAddress, ClientState> clients;
	private World world;
	private long lastWorldUpdateTime;
	private boolean shutdown;

	/** Creates a new Server with the provided local address.
	 * 
	 * @param bind the local address of the server.
	 * @return a new Server.
	 * @throws IOException if an error occurs when opening the network channel.
	 */
	public static Server start(InetSocketAddress bind) throws IOException {
		return new Server(bind);
	}

	private Server(InetSocketAddress bind) throws IOException {
		channel = DatagramChannel.open(NetworkUtility.protocolOf(bind.getAddress())).bind(bind);
		channel.configureBlocking(false);
		clients = new HashMap<>();
		sendBuffer = ByteBuffer.allocateDirect(MAX_PACKET_SIZE);
		receiveBuffer = ByteBuffer.allocateDirect(MAX_PACKET_SIZE);
		responseBuffer = ByteBuffer.allocateDirect(HEADER_SIZE);
	}
	
	public InetSocketAddress getAddress() throws IOException {
		return (InetSocketAddress)channel.getLocalAddress();
	}
	
	public void startShutdown() {
		shutdown = true;
		for(ClientState client : clients.values()) {
			kickClient(client, "server shutting down");
		}
	}
	
	private static void log(String message) {
		System.out.println(Utility.formatCurrentTime() + " " + message);
	}
	
	public void close() throws IOException {
		channel.close();
	}

	public boolean isOpen() {
		return channel.isOpen();
	}

	public String[] listClients() {
		return clients.values()
			.stream()
			.map(ClientState::formattedName)
			.toArray(String[]::new);
	}

	public int getClientCount() {
		return clients.size();
	}

	public void setCurrentWorld(World world) {
		Objects.requireNonNull(world).setRemoveEntities(this::broadcastRemoveEntity);
		this.world = world;
		this.lastWorldUpdateTime = System.nanoTime();
	}
	
	public World world() {
		return world;
	}
	
	private static void kickClient(ClientState client, String reason) {
		byte[] message = reason.getBytes(CHARSET);
		byte[] packet = new byte[message.length + 6];
		Bytes.putShort(packet, 0, TYPE_SERVER_CLIENT_DISCONNECT);
		Bytes.putInteger(packet, 2, message.length);
		Bytes.copy(message, packet, 6);
		sendUnsafe(client, packet, true);
		client.status = ClientState.STATUS_KICKED;
		client.disconnectReason = "kicked by server: " + reason;
	}

	private void onReceive(ClientState client, ByteBuffer packet) {
		try {
			client.lastMessageReceiveTime = System.nanoTime();
			short type = packet.getShort();
			onReceive(type, client, packet);
		} catch(ClientBadDataException e) {
			client.status = ClientState.STATUS_KICKED;
			client.disconnectReason = "Received bad data - " + e.getMessage();
			log("Server received bad data from client: " + e.getMessage());
		}
	}
	
	private void onReceive(short type, ClientState client, ByteBuffer packet) {
		switch(type) {
			case TYPE_CLIENT_CONNECT_REQUEST -> processClientConnectRequest(client);
			case TYPE_CLIENT_DISCONNECT -> processClientDisconnect(client);
			case TYPE_CLIENT_BREAK_BLOCK -> processClientBreakBlock(client, packet);
			case TYPE_CLIENT_PLACE_BLOCK -> processClientPlaceBlock(client, packet);
			case TYPE_CLIENT_BOMB_THROW -> processClientThrowBomb(client, packet);
			case TYPE_CLIENT_WORLD_BUILT -> client.status = ClientState.STATUS_IN_GAME;
			case TYPE_CLIENT_PLAYER_STATE -> processClientPlayerState(client, packet);
			case TYPE_PING -> {} //do nothing
			default -> throw new ClientBadDataException("received unknown protocol " + type);
		}
	}
	
	private long lastClientsUpdate;
	private static final int ENTITY_UPDATE_HEADER_SIZE = 6;
	private static final int BYTES_PER_ENTITY = 20;
	private static final int MAX_ENTITIES_PER_PACKET = 
			(MAX_MESSAGE_LENGTH - ENTITY_UPDATE_HEADER_SIZE)/BYTES_PER_ENTITY;
	
	public void update() throws IOException {
		receive();
		handleClientStatuses();
		if(shutdown && getClientCount() == 0) {
			close();
		} else {
			lastWorldUpdateTime = Utility.updateWorld(
				world,
				lastWorldUpdateTime,
				MAX_UPDATE_TIMESTEP
			);
			
			if(Utility.nanosSince(lastClientsUpdate) > NETWORK_SEND_INTERVAL_NANOSECONDS) {
				sendEntityUpdates();
				lastClientsUpdate = System.nanoTime();
			}
			
			for(ClientState client : clients.values()) {
				sendQueued(client);
			}
		}
	}

	private void handleClientStatuses() {
		var iterator = clients.values().iterator();
		while(iterator.hasNext()) {
			ClientState client = iterator.next();
			switch(client.status) {
				case ClientState.STATUS_CONNECTED -> {
					//do nothing because client might be deserializing world
				}
				
				case ClientState.STATUS_IN_GAME -> {
					client.checkReceiveTimeout();
				}
				
				case ClientState.STATUS_TIMED_OUT -> {
					iterator.remove();
					handleDisconnect(client);
				}
				
				case ClientState.STATUS_KICKED, ClientState.STATUS_SELF_DISCONNECTED -> {
					if(client.sendQueue.isEmpty()) iterator.remove();
					handleDisconnect(client);
				}
				
				case ClientState.STATUS_REJECTED -> {
					if(client.sendQueue.isEmpty()) iterator.remove();
				}
				
				default -> throw new UnsupportedOperationException("Unknown client status");
			}
		}
	}
	
	private void handleDisconnect(ClientState client) {
		broadcastAndPrint(new StringBuilder(
			NetworkUtility.formatAddress(client.address))
			.append(" disconnected (reason: '")
			.append(client.disconnectReason)
			.append("', ")
			.append(clients.size())
			.append(" players connected)")
			.toString()
		);
		
		if(client.player != null) {
			world.remove(client.player);
			broadcastRemoveEntity(client.player);
		}
	}
	
	private void sendEntityUpdates() {
		int entitiesRemaining = world.entities();
		Iterator<Entity> iterator = world.iterator();
		while(iterator.hasNext()) {
			
			int count = Math.min(entitiesRemaining, MAX_ENTITIES_PER_PACKET);
			int index = 0;
			byte[] packet = new byte[ENTITY_UPDATE_HEADER_SIZE + count * BYTES_PER_ENTITY];
			Bytes.putShort(packet, 0, TYPE_SERVER_ENTITY_UPDATE);
			Bytes.putInteger(packet, 2, count);
			while(index < count) {
				Entity entity = iterator.next();
				populateEntityUpdate(packet, ENTITY_UPDATE_HEADER_SIZE + index * BYTES_PER_ENTITY, entity);
				entitiesRemaining--;
				index++;
				if(entity instanceof PlayerEntity p) {
					broadcastPlayerState(null, p);
				}
			}
			broadcastUnsafe(packet, false, client -> client.status == ClientState.STATUS_IN_GAME);
		}
	}
	
	private static void populateEntityUpdate(byte[] packet, int index, Entity e) {
		Bytes.putInteger(packet, index + 0, e.getID());
		Bytes.putFloat(packet, index + 4, e.getPositionX());
		Bytes.putFloat(packet, index + 8, e.getPositionY());
		Bytes.putFloat(packet, index + 12, e.getVelocityX());
		Bytes.putFloat(packet, index + 16, e.getVelocityY());
	}

	public void broadcastConsoleMessage(String message) {
		broadcastUnsafe(Protocol.buildConsoleMessage(message), true, c -> true);
	}

	private void broadcastAndPrint(String consoleMessage) {
		broadcastUnsafe(Protocol.buildConsoleMessage(consoleMessage), true, c -> true);
		log(consoleMessage);
	}

	private void processClientPlayerState(ClientState client, ByteBuffer packet) {
		PlayerEntity player = client.player;
		if(player == null)
			throw new ClientBadDataException("client has no associated player to perform an action");
		if(world.contains(player)) {
			short state = packet.getShort();
			Protocol.PlayerState.updatePlayer(player, state);
			broadcastPlayerState(client, player);
			//TODO for now ignore the primary/secondary actions
		} //else what is the point of the player performing the action
	}

	private void broadcastPlayerState(ClientState client, PlayerEntity player) {
		byte[] message = new byte[8];
		Bytes.putShort(message, 0, TYPE_CLIENT_PLAYER_STATE);
		Bytes.putInteger(message, 2, player.getID());
		Bytes.putShort(message, 6, Protocol.PlayerState.getState(player, false, false));
		broadcastUnreliable(message, message.length, recipient -> !recipient.equals(client));
	}

	public void printDebug(PrintStream out) {
		if(clients.isEmpty()) {
			out.println("No connected clients.");
		} else {
			for(ClientState client : clients.values()) {
				out.println(client);
			}
		}
	}

	private static void processClientDisconnect(ClientState client) {
		client.status = ClientState.STATUS_SELF_DISCONNECTED;
		client.disconnectReason = "self disconnect";
	}

	private void processClientBreakBlock(ClientState client, ByteBuffer data) {
		int x = data.getInt();
		int y = data.getInt();
		//TODO sync block break cooldowns (System.currentTimeMillis() + cooldown)
		if(!world.getForeground().isValid(x, y))
			throw new ClientBadDataException("client sent invalid block coordinates x=" + x + " y=" + y);
		if(Utility.canBreak(client.player, client.lastBlockBreakTime, world, x, y)) {
			breakBlock(x, y);
			client.lastBlockBreakTime = System.nanoTime();
		}
	}
	
	private void processClientPlaceBlock(ClientState client, ByteBuffer packet) {
		int x = packet.getInt();
		int y = packet.getInt();
		PlayerEntity player = client.player;
		//TODO sync block place cooldowns (System.currentTimeMillis() + cooldown)
		if(!world.getForeground().isValid(x, y))
			throw new ClientBadDataException("client sent invalid block coordinates x=" + x + " y=" + y);
		if(Utility.canPlace(player, client.lastBlockPlaceTime, world, x, y)) {
			Block blockType = switch(player.selected()) {
				case 1 -> GrassBlock.INSTANCE;
				case 2 -> DirtBlock.INSTANCE;
				default -> null;
			};
			
			if(blockType != null) {
				world.getForeground().place(world, x, y, blockType);
				broadcastPlaceBlock(blockType, x, y);
				client.lastBlockPlaceTime = System.nanoTime();	
			}
		}		
	}

	public void broadcastRemoveBlock(int x, int y) {
		byte[] packet = new byte[10];
		Bytes.putShort(packet, 0, TYPE_SERVER_REMOVE_BLOCK);
		Bytes.putInteger(packet, 2, x);
		Bytes.putInteger(packet, 6, y);
		broadcastUnsafe(packet, true, c -> true);
	}
	
	private void broadcastPlaceBlock(Block block, int x, int y) {
		byte[] blockData = serialize(block, true);
		byte[] packet = new byte[10 + blockData.length];
		Bytes.putShort(packet, 0, TYPE_SERVER_PLACE_BLOCK);
		Bytes.putInteger(packet, 2, x);
		Bytes.putInteger(packet, 6, y);
		Bytes.copy(blockData, packet, 10);
		broadcastUnsafe(packet, true, c -> true);
	}
	
	private static final float 
		BLOCK_DROP_VELOCITY = Utility.convertPerSecondToPerNano(7f);

	public void breakBlock(int blockX, int blockY) {
		Block block = world.getForeground().destroy(world, blockX, blockY);
		broadcastRemoveBlock(blockX, blockY);
		if(block != null) {
			var drop = new ItemEntity<>(world.nextEntityID(), new BlockItem(block), blockX, blockY);
			float angle = Utility.random(0, Math.PI);
			drop.setVelocityX((float)Math.cos(angle) * BLOCK_DROP_VELOCITY);
			drop.setVelocityY((float)Math.sin(angle) * BLOCK_DROP_VELOCITY);
			world.add(drop);
			broadcastAddEntity(drop);
		}
	}
	
	private void processClientThrowBomb(ClientState client, ByteBuffer data) {
		if(Utility.canThrow(client.lastThrowTime)) {
			BombEntity bomb = new ServerBombEntity(this, world.nextEntityID());
			bomb.setPositionX(client.player.getPositionX());
			bomb.setPositionY(client.player.getPositionY());
			Utility.launchAtAngle(bomb, data.getFloat(), BOMB_THROW_VELOCITY);
			world.add(bomb);
			broadcastAddEntity(bomb);
			client.lastThrowTime = System.nanoTime();
		}
	}

	private void processClientConnectRequest(ClientState client) {
		if(world != null) {
			ServerPlayerEntity player = new ServerPlayerEntity(world.nextEntityID());
			placePlayer(player, world.getForeground());
			world.add(player);
			client.player = player;
			//send entity to already connected players
			broadcastUnsafe(buildAddEntity(player), true, c -> c.status == ClientState.STATUS_IN_GAME);
			
			//TODO don't send world size in ack, takes to long to serialize entire world
			for(byte[] packet : buildAcknowledgementPackets(world, player.getID())) {
				sendUnsafe(client, packet, true);
			}

			//sendPlayerID(client, player); //send id of player entity which was sent in world data
			log(NetworkUtility.formatAddress(client.address) + " joined (" 
					+ getClientCount() + " player(s) connected)");
		} else {
			byte[] response = new byte[3];
			Bytes.putShort(response, 0, TYPE_SERVER_CONNECT_ACKNOWLEDGMENT);
			response[2] = CONNECT_STATUS_REJECTED;
			sendReliable(client, response);
			client.status = ClientState.STATUS_REJECTED;
			client.disconnectReason = "client rejected";
		}
	}

	private static void placePlayer(Entity player, BlockGrid grid) {
		float posX = grid.getWidth()/2;
		player.setPositionX(posX);
		for(int blockY = grid.getHeight() - 1; blockY > 0; blockY--) {
			if(grid.isBlock(posX, blockY)) {
				player.setPositionY(blockY + 0.5f + player.getHeight()/2);
				return;
			}
		}
		player.setPositionY(grid.getHeight());
	}

	private static byte[][] buildAcknowledgementPackets(World world, int playerID) {
		byte[] worldBytes = serialize(world, COMPRESS_WORLD_DATA);
		byte[][] packets = Bytes.split(worldBytes, MAX_MESSAGE_LENGTH - 2, 2, 1);
		packets[0] = buildConnectAcknowledgement(worldBytes.length, playerID);
		for(int i = 1; i < packets.length; i++) {
			Bytes.putShort(packets[i], 0, TYPE_SERVER_WORLD_DATA);
		}
		return packets;
	}

	private static byte[] buildConnectAcknowledgement(int worldSize, int playerID) {
		byte[] head = new byte[2 + 1 + 4 + 4];
		Bytes.putShort(head, 0, TYPE_SERVER_CONNECT_ACKNOWLEDGMENT);
		head[2] = CONNECT_STATUS_WORLD;
		Bytes.putInteger(head, 3, worldSize);
		Bytes.putInteger(head, 7, playerID);
		return head;
	}

	private static byte[] serialize(Transportable object, boolean compress) {
		byte[] serialized = SerializationProvider.getProvider().serialize(object);
		return compress ? Bytes.compress(serialized) : serialized;
	}

	public void broadcastAddEntity(Entity e) {
		broadcastUnsafe(buildAddEntity(e), true, c -> true);
	}
	
	private static byte[] buildAddEntity(Entity e) {
		byte[] entity = SerializationProvider.getProvider().serialize(e);
		boolean compress = entity.length > MAX_MESSAGE_LENGTH - 3;
		entity = compress ? Bytes.compress(entity) : entity;
		byte[] packet = new byte[3 + entity.length];
		Bytes.putShort(packet, 0, TYPE_SERVER_ADD_ENTITY);
		Bytes.putBoolean(packet, 2, compress);
		Bytes.copy(entity, packet, 3);
		return packet;
	}

	public void broadcastRemoveEntity(Entity e) {
		byte[] packet = new byte[2 + 4];
		Bytes.putShort(packet, 0, TYPE_SERVER_REMOVE_ENTITY);
		Bytes.putInteger(packet, 2, e.getID());
		broadcastUnsafe(packet, true, c -> true);
	}

	private void broadcastUnsafe(byte[] data, boolean reliable, Predicate<ClientState> sendToClient) {
		SendPacket packet = new SendPacket(data, reliable);
		for(ClientState client : clients.values()) {
			if(!client.isDisconnectState() && sendToClient.test(client))
				client.sendQueue.add(packet);
		}
	}

	private void broadcastUnreliable(byte[] data, int length, Predicate<ClientState> sendToClient) {
		SendPacket packet = new SendPacket(Arrays.copyOfRange(data, 0, length), false);
		for(ClientState client : clients.values()) {
			if(sendToClient.test(client) && !client.isDisconnectState()) {
				client.sendQueue.add(packet);
			}
		}
	}

	private static void sendReliable(ClientState client, byte[] data) {
		sendUnsafe(client, data.clone(), true);
	}

	private static void sendUnsafe(ClientState client, byte[] data, boolean reliable) {
		client.sendQueue.add(new SendPacket(data, reliable));
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
			return "SendPacket[reliable: " + reliable + " size:" + data.length + "]";
		}
	}

	private void receive() throws IOException {
		InetSocketAddress sender;
		while((sender = (InetSocketAddress)channel.receive(receiveBuffer)) != null) {
			processPacket(sender);
		}
	}

	private void sendResponse(InetSocketAddress recipient, ByteBuffer sendBuffer, int receivedMessageID) 
			throws IOException {
		channel.send(sendBuffer.put(RESPONSE_TYPE).putInt(receivedMessageID).flip(), recipient);
		sendBuffer.clear();
	}

	private void processPacket(InetSocketAddress sender) throws IOException {
		ByteBuffer buffer = receiveBuffer;
		buffer.flip(); //flip to set limit and prepare to read packet data
		if(buffer.limit() >= HEADER_SIZE) {
			byte type = buffer.get(); //type of message (RESPONSE, RELIABLE, UNRELIABLE)
			int messageID = buffer.getInt(); //received ID or messageID for ack.
			ClientState client = clients.computeIfAbsent(sender, addr -> new ClientState(addr));
			switch(type) {
				case RESPONSE_TYPE -> {
					if(client.sendReliableID == messageID) {
						client.ping = Utility.nanosSince(client.sendStartTime);
						client.sendAttempts = 0;
						client.sendReliableID++;
						client.sendQueue.poll(); //remove and continue processing
					} //else drop the response
				}

				case RELIABLE_TYPE -> {
					if(messageID == client.receiveReliableID) {
						//if the message is the next one, process it and update last message
						sendResponse(sender, responseBuffer, messageID);
						client.receiveReliableID++;
						onReceive(client, buffer.position(HEADER_SIZE));
					} else if(messageID < client.receiveReliableID) { //message already received
						sendResponse(sender, responseBuffer, messageID);
					} //else: message received too early
				}

				case UNRELIABLE_TYPE -> {
					if(messageID >= client.receiveUnreliableID) {
						client.receiveUnreliableID = messageID + 1;
						onReceive(client, buffer.position(HEADER_SIZE));
					} //else: message is outdated
				}
			}
		}
		buffer.clear(); //clear to prepare for next receive
	}

	private static void setupSendBuffer(ByteBuffer sendBuffer, byte type, int id, byte[] data) {
		sendBuffer.put(type).putInt(id).put(data).flip();
	}

	private void sendReliableImpl(ClientState client, SendPacket packet) throws IOException {
		client.sendAttempts++;
		setupSendBuffer(sendBuffer, RELIABLE_TYPE, client.sendReliableID, packet.data);
		channel.send(sendBuffer, client.address);
		sendBuffer.clear();
	}
	
	private void sendQueued(ClientState client) throws IOException {
		 //TODO create interface type that writes directly to send buffer
		Queue<SendPacket> queue = client.sendQueue;
		while(!queue.isEmpty()) {
			if(queue.peek().reliable) {
				if(client.sendAttempts == 0) { //initialize
					client.sendStartTime = System.nanoTime();
					sendReliableImpl(client, queue.peek());
				} else if(client.sendAttempts <= RESEND_COUNT) { //check if not timed out
					if(Utility.resendIntervalElapsed(client.sendStartTime, client.sendAttempts)) {
						sendReliableImpl(client, queue.peek());
					}
				} else { //client has timed out
					client.handleSendTimeout();
				}
				break;
			} else {
				setupSendBuffer(sendBuffer, UNRELIABLE_TYPE, client.sendUnreliableID++, queue.poll().data);
				channel.send(sendBuffer, client.address);
				sendBuffer.clear();
			}
		}
	}

	private static final class ClientState {
		static final byte
			STATUS_CONNECTED = 0, //after the client acks the connect ack
			STATUS_TIMED_OUT = 1, //if the client doesn't send an ack within ack interval
			STATUS_KICKED = 2, //if server kicks a player or shuts down
			STATUS_SELF_DISCONNECTED = 3, //if the client manually disconnects
			STATUS_REJECTED = 4, //if the server rejects the client
			STATUS_IN_GAME = 5; //if the client has received the world and player and notifies server

		int receiveReliableID, receiveUnreliableID, sendUnreliableID, sendReliableID;
		int sendAttempts;
		long sendStartTime, lastMessageReceiveTime;
		final InetSocketAddress address;
		final Queue<SendPacket> sendQueue;
		byte status;

		/** Client reliable message round trip time in nanoseconds */
		long ping;
		
		/** Last player action times in nanoseconds offset */
		long lastBlockBreakTime, lastBlockPlaceTime, lastThrowTime;
		ServerPlayerEntity player;
		String disconnectReason;
		
		ClientState(InetSocketAddress address) {
			this.address = address;
			status = STATUS_CONNECTED;
			sendQueue = new ArrayDeque<>();
		}
		
		static String statusToString(byte status) {
			return switch(status) {
				case STATUS_CONNECTED -> 			"CONNECTED";
				case STATUS_TIMED_OUT -> 			"TIMED_OUT";
				case STATUS_KICKED -> 				"KICKED";
				case STATUS_SELF_DISCONNECTED -> 	"DISCONNECTED";
				case STATUS_REJECTED -> 			"REJECTED";
				case STATUS_IN_GAME -> 				"IN_GAME";
				default -> 							"ILLEGAL";
			};
		}
		
		private boolean isDisconnectState() {
			return switch(status) {
				case ClientState.STATUS_TIMED_OUT, ClientState.STATUS_KICKED, 
					ClientState.STATUS_SELF_DISCONNECTED, ClientState.STATUS_REJECTED -> true;
				case ClientState.STATUS_CONNECTED, ClientState.STATUS_IN_GAME -> false;
				default -> false;
			};
		}
		
		private void checkReceiveTimeout() {
			long delta = Utility.nanosSince(lastMessageReceiveTime);
			if(delta > TIMEOUT_DISCONNECT) {
				status = STATUS_TIMED_OUT;
				disconnectReason = "server didn't receive message in " + Utility.formatTime(delta);
			}
		}

		private void handleSendTimeout() {
			status = STATUS_TIMED_OUT;
			disconnectReason = "client didn't respond to reliable message";
		}
		
		public String formattedName() {
			return NetworkUtility.formatAddress(address) + ": " + ClientState.statusToString(status);
		}

		@Override
		public String toString() {
			return new StringBuilder()
				.append("ClientState[address: ")
				.append(NetworkUtility.formatAddress(address))
				.append(" rSend: ")
				.append(sendReliableID)
				.append(" rReceive: ")
				.append(receiveReliableID)
				.append(" ping: ")
				.append(Utility.formatTime(ping))
				.append(" queuedSend: ")
				.append(sendQueue.size())
				.append(']')
				.toString();
		}
	}
}
