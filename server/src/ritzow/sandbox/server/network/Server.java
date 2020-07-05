package ritzow.sandbox.server.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Transportable;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.ReceivePacket;
import ritzow.sandbox.network.SendPacket;
import ritzow.sandbox.server.SerializationProvider;
import ritzow.sandbox.server.world.entity.ServerPlayerEntity;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.block.DirtBlock;
import ritzow.sandbox.world.block.GrassBlock;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.entity.PlayerEntity;
import ritzow.sandbox.world.item.BlockItem;

import static ritzow.sandbox.network.Protocol.*;
import static ritzow.sandbox.server.network.ClientState.*;

/** The server manages connected game clients, sends game updates,
 * receives client input, and broadcasts information for clients. */
public class Server {
	//TODO server needs to drop unreliable packets if it receives too many (ie client sending at 120 fps while server is 60 fps)
	//TODO implement encryption on client and server https://howtodoinjava.com/security/java-aes-encryption-example/
	private static final long NETWORK_SEND_INTERVAL_NANOSECONDS = Utility.millisToNanos(200);
	private static final long PLAYER_STATE_BROADCAST_INTERVAL = Utility.millisToNanos(100);

	private final DatagramChannel channel;
	private final ByteBuffer receiveBuffer, sendBuffer;
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
	}

	public InetSocketAddress getAddress() throws IOException {
		return (InetSocketAddress)channel.getLocalAddress();
	}

	private static void log(String message) {
		System.out.println(Utility.formatCurrentTime() + " " + message);
	}

	/** Starts the shutdown process of kicking all the clients off the server **/
	public void shutdown() {
		shutdown = true;
		for(ClientState client : clients.values()) {
			kickClient(client, "Server shutting down");
		}
	}

	public void close() throws IOException {
		channel.close();
	}

	public boolean isOpen() {
		return channel.isOpen();
	}

	public Stream<String> clientNames() {
		return clients.values().stream()
			.map(ClientState::formattedName);
	}

	public int getClientCount() {
		return clients.size();
	}

	public void setCurrentWorld(World world) {
		world.setRemoveEntities(this::broadcastRemoveEntity);
		this.world = world;
		this.lastWorldUpdateTime = System.nanoTime();
	}

	public World world() {
		return world;
	}

	private static void kickClient(ClientState client, String reason) {
		client.status = STATUS_KICKED;
		sendDisconnect(client, "kicked for " + reason, true);
	}

	private static void sendDisconnect(ClientState client, String reason, boolean reliable) {
		byte[] message = reason.getBytes(CHARSET);
		byte[] packet = new byte[message.length + 6];
		Bytes.putShort(packet, 0, TYPE_SERVER_CLIENT_DISCONNECT);
		Bytes.putInteger(packet, 2, message.length);
		Bytes.copy(message, packet, 6);
		if(reliable)
			sendReliableUnsafe(client, packet);
		else
			sendUnreliableUnsafe(client, packet);
	}

	private void onReceive(short type, ClientState client, ByteBuffer packet) {
		switch(client.status) {
			case STATUS_CONNECTED -> {
				switch(type) {
					case TYPE_CLIENT_CONNECT_REQUEST -> processClientConnectRequest(client);
					case TYPE_CLIENT_WORLD_BUILT -> processClientWorldBuilt(client);
					default -> {
						client.status = STATUS_INVALID;
						sendDisconnect(client, "invalid", false);
						log("Removed invalid client " + client);
					}
				}
			}

			case STATUS_IN_GAME -> {
				switch(type) {
					case TYPE_CLIENT_DISCONNECT -> processClientSelfDisconnect(client);
					case TYPE_CLIENT_BREAK_BLOCK -> processClientBreakBlock(client, packet);
					case TYPE_CLIENT_PLACE_BLOCK -> processClientPlaceBlock(client, packet);
					case TYPE_CLIENT_PLAYER_STATE -> processClientPlayerState(client, packet);
					case TYPE_PING -> {} //do nothing
					default -> throw new ClientBadDataException("received unknown protocol " + type);
				} //^^ TODO unify bad data exception vs invalid, etc.
			}

			case STATUS_TIMED_OUT,
				STATUS_INVALID,
				STATUS_REJECTED,
				STATUS_LEAVE,
				STATUS_KICKED -> sendDisconnect(client, "already disconnected", false);
		}
	}

	private long lastClientsUpdate;
	private static final int
		ENTITY_UPDATE_HEADER_SIZE = 6,
		BYTES_PER_ENTITY = 20,
		MAX_ENTITIES_PER_PACKET = (MAX_MESSAGE_LENGTH - ENTITY_UPDATE_HEADER_SIZE)/BYTES_PER_ENTITY;

	public void update() throws IOException {
		receive();
		handleClientStatus();
		if(shutdown) {
			if(!clients.isEmpty()) {
				sendQueued();
			} else {
				close();
			}
		} else {
			lastWorldUpdateTime = Utility.updateWorld(
				world,
				lastWorldUpdateTime,
				MAX_UPDATE_TIMESTEP
			);

			if(Utility.nanosSince(lastClientsUpdate) > NETWORK_SEND_INTERVAL_NANOSECONDS) {
				lastClientsUpdate = System.nanoTime();
				sendEntityUpdates();
			}
			sendQueued();
		}
	}

	//TODO deal with limbo states such as when client responds, but isn't actually doing anything
	private void handleClientStatus() { //TODO this is not allowing disconnect messages to be sent!
		var iterator = clients.values().iterator();
		while(iterator.hasNext()) {
			ClientState client = iterator.next();
			switch(client.status) {
				//check that the client is still sending pings
				case STATUS_CONNECTED -> checkTimeout(client);

				case STATUS_IN_GAME -> {
					if(checkTimeout(client)) {
						iterator.remove();
						handleDisconnect(client);
					} else if(Utility.nanosSince(client.lastPlayerStateUpdate)
						> PLAYER_STATE_BROADCAST_INTERVAL) {
						client.lastPlayerStateUpdate = System.nanoTime();
						broadcastUnreliable(
							buildPlayerStateMessage(client.player), ClientState::inGame);
					}
				}

				case STATUS_KICKED, STATUS_LEAVE, STATUS_REJECTED -> {
					if(!client.hasPending()) {
						iterator.remove();
						handleDisconnect(client);
					} else {
						log(client.sendQueue.toString());
					}
				}

				case STATUS_TIMED_OUT -> {
					iterator.remove();
					handleDisconnect(client);
				}

				case STATUS_INVALID -> iterator.remove();

				default -> throw new UnsupportedOperationException("Unknown client status");
			}
		}
	}

	private static boolean checkTimeout(ClientState client) {
		long delta = Utility.nanosSince(client.lastMessageReceiveTime);
		if(delta > TIMEOUT_DISCONNECT) {
			client.status = STATUS_TIMED_OUT;
			client.disconnectReason = "server didn't receive message in " + Utility.formatTime(delta);
			return true;
		}
		return false;
	}

	private void handleDisconnect(ClientState client) {
		broadcastAndPrint(
				NetworkUtility.formatAddress(client.address) +
				" disconnected (reason: '" +
				client.disconnectReason +
				"', " + clients.size() +
				" players connected)"
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
			byte[] packet = new byte[ENTITY_UPDATE_HEADER_SIZE + count * BYTES_PER_ENTITY];
			Bytes.putShort(packet, 0, TYPE_SERVER_ENTITY_UPDATE);
			Bytes.putInteger(packet, 2, count);
			for(int index = 0; index < count; index++) {
				Entity entity = iterator.next();
				populateEntityUpdate(packet, ENTITY_UPDATE_HEADER_SIZE + index * BYTES_PER_ENTITY, entity);
				entitiesRemaining--;
			}
			broadcastUnsafe(packet, false, ClientState::inGame);
		}
	}

	private static void populateEntityUpdate(byte[] packet, int index, Entity e) {
		Bytes.putInteger(packet, index + 0, e.getID());
		Bytes.putFloat(packet, index + 4, e.getPositionX());
		Bytes.putFloat(packet, index + 8, e.getPositionY());
		Bytes.putFloat(packet, index + 12, e.getVelocityX());
		Bytes.putFloat(packet, index + 16, e.getVelocityY());
	}

	public void broadcastConsoleMessage(String message) { //TODO still send for kicked, etc.
		broadcastUnsafe(buildConsoleMessage(message), true, ClientState::isNotDisconnectState);
	}

	private void broadcastAndPrint(String consoleMessage) {
		broadcastConsoleMessage(consoleMessage);
		log(consoleMessage);
	}

	private void processClientWorldBuilt(ClientState client) {
		client.status = STATUS_IN_GAME;
		log(NetworkUtility.formatAddress(client.address) + " joined ("
			+ getClientCount() + " player(s) connected)");
	}

	private static void processClientSelfDisconnect(ClientState client) {
		client.status = STATUS_LEAVE;
		client.disconnectReason = "self disconnect";
	}

	private void processClientPlayerState(ClientState client, ByteBuffer packet) {
		PlayerEntity player = client.player;
		client.lastPlayerStateUpdate = System.nanoTime();
		if(player == null)
			throw new ClientBadDataException("client has no associated player to perform an action");
		if(world.contains(player)) {
			short state = packet.getShort();
			PlayerState.updatePlayer(player, state);
			var msg = buildPlayerStateMessage(client.player);
			broadcastUnreliable(msg, r -> r.inGame() && !r.equals(client));
			//TODO for now ignore the primary/secondary actions
		} else {
			//else what is the point of the player performing the action
			throw new ClientBadDataException("client player is not in game world");
		}
	}

	private static byte[] buildPlayerStateMessage(PlayerEntity player) {
		byte[] message = new byte[8];
		Bytes.putShort(message, 0, TYPE_CLIENT_PLAYER_STATE);
		Bytes.putInteger(message, 2, player.getID());
		Bytes.putShort(message, 6, PlayerState.getState(player, false, false));
		return message;
	}

	public String getDebugInfo() {
		if(clients.isEmpty()) {
			return "No connected clients.";
		} else {
			StringJoiner joiner = new StringJoiner("\n");
			for(ClientState client : clients.values()) {
				joiner.add(client.toString());
			}
			return joiner.toString();
		}
	}

	private void processClientBreakBlock(ClientState client, ByteBuffer data) {
		int x = data.getInt();
		int y = data.getInt();
		if(!world.getForeground().isValid(x, y))
			throw new ClientBadDataException("client sent invalid block coordinates x=" + x + " y=" + y);
		if(Utility.canBreak(client.player, client.lastBlockBreakTime, world, x, y)) {
			//TODO sync block break cooldowns (System.currentTimeMillis() + cooldown)
			sendBreakCooldown(client, System.currentTimeMillis());
			breakBlock(x, y);
			client.lastBlockBreakTime = System.nanoTime();
		}
	}

	private static void sendBreakCooldown(ClientState client, long currentTimeMillis) {
		byte[] packet = new byte[2 + 8];
		Bytes.putShort(packet, 0, TYPE_SERVER_CLIENT_BREAK_BLOCK_COOLDOWN);
		Bytes.putLong(packet, 2, currentTimeMillis);
		sendReliableSafe(client, packet);
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
		broadcastUnsafe(packet, true, ClientState::isNotDisconnectState);
	}

	private void broadcastPlaceBlock(Block block, int x, int y) {
		byte[] blockData = serialize(block, false);
		byte[] packet = new byte[10 + blockData.length];
		Bytes.putShort(packet, 0, TYPE_SERVER_PLACE_BLOCK);
		Bytes.putInteger(packet, 2, x);
		Bytes.putInteger(packet, 6, y);
		Bytes.copy(blockData, packet, 10);
		broadcastUnsafe(packet, true, ClientState::isNotDisconnectState);
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

	private void processClientConnectRequest(ClientState client) {
		if(world != null) {
			ServerPlayerEntity player = new ServerPlayerEntity(world.nextEntityID());
			placePlayer(player, world.getForeground());
			world.add(player);
			client.player = player;
			//send entity to already connected players
			broadcastUnsafe(buildAddEntity(player), true, ClientState::inGame);

			log("Serializing world to send to " + client.formattedName());
			//TODO don't send world size in ack, takes too long to serialize entire world
			for(byte[] packet : buildAcknowledgementPackets(world, player.getID())) {
				sendReliableUnsafe(client, packet);
			}
			log("World serialized and sent to " + client.formattedName());
		} else {
			byte[] response = new byte[3];
			Bytes.putShort(response, 0, TYPE_SERVER_CONNECT_ACKNOWLEDGMENT);
			response[2] = CONNECT_STATUS_REJECTED;
			sendReliableUnsafe(client, response);
			client.status = STATUS_REJECTED;
			client.disconnectReason = "client rejected";
		}
	}

	private static void placePlayer(Entity player, BlockGrid grid) {
		float posX = grid.getWidth()/2f;
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
		broadcastUnsafe(buildAddEntity(e), true, ClientState::isNotDisconnectState);
	}

	private static byte[] buildAddEntity(Entity e) {
		byte[] entity = SerializationProvider.getProvider().serialize(e);
		boolean compress = entity.length > MAX_MESSAGE_LENGTH - 3;
		entity = compress ? Bytes.compress(entity) : entity;
		byte[] packet = new byte[3 + entity.length];
		Bytes.putShort(packet, 0, TYPE_SERVER_CREATE_ENTITY);
		Bytes.putBoolean(packet, 2, compress);
		Bytes.copy(entity, packet, 3);
		return packet;
	}

	public void broadcastRemoveEntity(Entity e) {
		byte[] packet = new byte[2 + 4];
		Bytes.putShort(packet, 0, TYPE_SERVER_DELETE_ENTITY);
		Bytes.putInteger(packet, 2, e.getID());
		broadcastUnsafe(packet, true, ClientState::isNotDisconnectState);
	}

	private void broadcastUnsafe(byte[] data, boolean reliable, Predicate<ClientState> sendToClient) {
		for(ClientState client : clients.values()) {
			if(sendToClient.test(client)) {
				client.send(data, reliable);
			}
		}
	}

	private void broadcastUnreliable(byte[] data, Predicate<ClientState> sendToClient) {
		broadcastUnreliable(data, data.length, sendToClient);
	}

	private void broadcastUnreliable(byte[] data, int length, Predicate<ClientState> sendToClient) {
		byte[] copy = Arrays.copyOfRange(data, 0, length);
		for(ClientState client : clients.values()) {
			if(sendToClient.test(client)) {
				client.send(copy, false);
			}
		}
	}

	private static void sendReliableSafe(ClientState client, byte[] data) {
		client.send(data.clone(), true);
	}

	private static void sendUnreliableUnsafe(ClientState client, byte[] data) {
		client.send(data, false);
	}

	private static void sendReliableUnsafe(ClientState client, byte[] data) {
		client.send(data, true);
	}

	private void receive() throws IOException {
		InetSocketAddress sender;
		while((sender = (InetSocketAddress)channel.receive(receiveBuffer)) != null) {
			processPacket(sender);
			receiveBuffer.clear();
		}
	}

	private void sendResponse(InetSocketAddress recipient, ByteBuffer sendBuffer, int receivedMessageID) throws IOException {
		channel.send(sendBuffer.put(RESPONSE_TYPE).putInt(receivedMessageID).flip(), recipient);
		sendBuffer.clear();
	}

	//use min heap (PriorityQueue) to keep messages in order while queued for processing
	//if message received is next message, don't bother putting it in queue
	private void processPacket(InetSocketAddress sender) throws IOException {
		if(receiveBuffer.flip().limit() >= MIN_PACKET_SIZE) { //check that packet is large enough
			ClientState client = clients.computeIfAbsent(sender, ClientState::new);
			client.lastMessageReceiveTime = System.nanoTime();
			byte type = receiveBuffer.get(); //type of message (RESPONSE, RELIABLE, UNRELIABLE)
			int messageID = receiveBuffer.getInt();
			switch(type) {
				case RESPONSE_TYPE -> {
					//remove packet from send queue once acknowledged
					Iterator<SendPacket> packets = client.sendQueue.iterator();
					while(packets.hasNext()) {
						SendPacket packet = packets.next();
						if(packet.messageID < messageID) {
							break;
						} else if(packet.messageID == messageID) {
							packets.remove();
							break;
						}
					}
				}

				//predecessorID needs to be replaced with messageID in some of this, fix
				case RELIABLE_TYPE -> {
					sendResponse(client.address, sendBuffer, messageID);
					if(messageID > client.headProcessedID) {
						int predecessorID = receiveBuffer.getInt();
						if(predecessorID <= client.headProcessedID) {
							//no need to add to the queue, this is the next message in the stream.
							sendResponse(client.address, sendBuffer, messageID);
							process(client, messageID, receiveBuffer);
						} else if(predecessorID > client.headProcessedID) {
							//predecessorID > headProcessedID
							//this will also happen if the message was already received
							//this wastes space in case of
							queueReceived(client, messageID, predecessorID, true, receiveBuffer);
						} //else error in packet data
					}
				}

				case UNRELIABLE_TYPE -> {
					//only process messages that aren't older than already processed messages
					//in order to keep all message processing in order
					if(messageID > client.headProcessedID) {
						int predecessorID = receiveBuffer.getInt();
						if(predecessorID > client.headProcessedID) {
							queueReceived(client, messageID, predecessorID, false, receiveBuffer);
						} else {
							//don't need to queue something that can be processed immediately
							process(client, messageID, receiveBuffer);
						}
					}
				}
			}
		}
	}

	private void queueReceived(ClientState client, int messageID, int predecessorID, boolean reliable, ByteBuffer data) {
		byte[] copy = new byte[data.remaining()];
		receiveBuffer.get(copy);
		client.receiveQueue.add(new ReceivePacket(messageID, predecessorID, reliable, copy));
	}

	private void process(ClientState client, int messageID, ByteBuffer receiveBuffer) {
		handleReceive(client, receiveBuffer);
		client.headProcessedID = messageID;
		ReceivePacket packet = client.receiveQueue.peek();
		while(packet != null && packet.predecessorReliableID() <= client.headProcessedID) {
			client.receiveQueue.poll();
			if(packet.messageID() > client.headProcessedID) {
				handleReceive(client, ByteBuffer.wrap(packet.data()));
				client.headProcessedID = packet.messageID();
			} //else was a duplicate
			packet = client.receiveQueue.peek();
		}
	}

	private void handleReceive(ClientState client, ByteBuffer packet) {
		try {
			onReceive(packet.getShort(), client, packet);
		} catch(ClientBadDataException e) {
			kickClient(client, "Received bad data - " + e.getMessage());
		}
	}

	private void sendQueued() throws IOException {
		//TODO maybe separate reliable messages into a different datastructure and use queue only for unreliables?
		for(ClientState client : clients.values()) {
			Iterator<SendPacket> packets = client.sendQueue.iterator();
			while(packets.hasNext()) {
				SendPacket packet = packets.next();
				if(packet.reliable) {
					long time = System.nanoTime();
					if(packet.lastSendTime == -1 || time - packet.lastSendTime > RESEND_INTERVAL) {
						sendBuffer(client, RELIABLE_TYPE, packet);
						packet.lastSendTime = time;
					}
				} else {
					//always remove unreliable messages, they will never be re-sent
					packets.remove();
					sendBuffer(client, UNRELIABLE_TYPE, packet);
				}
			}
		}
	}

	private void sendBuffer(ClientState client, byte type, SendPacket packet) throws IOException {
		channel.send(sendBuffer.put(type).putInt(packet.messageID).putInt(packet.lastReliableID).put(packet.data).flip(), client.address);
		sendBuffer.clear();
	}
}
