package ritzow.sandbox.server.network;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.function.Predicate;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Transportable;
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

//currently uses single-threaded receive processing, queued blocking message sending
/** The server manages connected game clients, sends game updates, receives client input, and broadcasts information for clients. */
public class Server {
	private static final long NETWORK_SEND_INTERVAL_NANOSECONDS = Utility.millisToNanos(100);
	private static final float BOMB_THROW_VELOCITY = 0.8f;

	private final DatagramChannel channel;
	private final ByteBuffer receiveBuffer, responseBuffer, sendBuffer;
	private final Map<InetSocketAddress, ClientState> clients;
	private World world;
	private long lastWorldUpdateTime;

	public static Server start(InetSocketAddress bind) throws IOException {
		return new Server(bind);
	}

	private Server(InetSocketAddress bind) throws IOException {
		channel = DatagramChannel.open(Utility.getProtocolFamily(bind.getAddress())).bind(bind);
		channel.configureBlocking(false);
		clients = new HashMap<>();
		sendBuffer = ByteBuffer.allocateDirect(Protocol.MAX_PACKET_SIZE);
		receiveBuffer = ByteBuffer.allocateDirect(Protocol.MAX_PACKET_SIZE);
		responseBuffer = ByteBuffer.allocateDirect(Protocol.HEADER_SIZE);
	}
	
	private static void log(String message) {
		System.out.println(Utility.formatCurrentTime() + " " + message);
	}

	public InetSocketAddress getAddress() throws IOException {
		return (InetSocketAddress)channel.getLocalAddress();
	}
	
	public void startShutdown() {
		for(ClientState client : clients.values()) {
			manualDisconnect(client, "server shutting down");
		}
		world = null; //TODO could cause lots of bugs
	}
	
	private void manualDisconnect(ClientState client, String reason) {
		sendReliable(client, buildManualDisconnect(reason));
		client.status = ClientState.STATUS_REMOVED;
		client.disconnectReason = "kicked by server: " + reason;
	}

	public void close() throws IOException {
		channel.close();
	}

	public boolean isOpen() {
		return channel.isOpen();
	}

	public String[] listClients() {
		return clients.values().stream().map(client -> client.formattedName()).toArray(count -> new String[count]);
	}

	public int getClientCount() {
		return clients.size();
	}

	public void setCurrentWorld(World world) {
		world.setRemoveEntities(this::broadcastRemoveEntity);
		this.world = world;
		this.lastWorldUpdateTime = System.nanoTime();
	}

	//TODO disconnect clients who don't connect properly
	private void onReceive(ClientState client, ByteBuffer packet) {
		try {
			short type = packet.getShort();
			switch(type) {
				case Protocol.TYPE_CLIENT_CONNECT_REQUEST -> sendClientConnectReply(client);
				case Protocol.TYPE_CLIENT_DISCONNECT -> processClientDisconnect(client);
				case Protocol.TYPE_CLIENT_BREAK_BLOCK -> processClientBreakBlock(client, packet);
				case Protocol.TYPE_CLIENT_PLACE_BLOCK -> processClientPlaceBlock(client, packet);
				case Protocol.TYPE_CLIENT_BOMB_THROW -> processClientThrowBomb(client, packet);
				case Protocol.TYPE_CLIENT_WORLD_BUILT -> client.status = ClientState.STATUS_IN_GAME;
				case Protocol.TYPE_CLIENT_PLAYER_STATE -> processClientPlayerState(client, packet);
				default -> throw new ClientBadDataException("received unknown protocol " + type);
			}
		} catch(ClientBadDataException e) {
			client.status = ClientState.STATUS_REMOVED;
			client.disconnectReason = "Received bad data - " + e.getMessage();
			log("Server received bad data from client: " + e.getMessage());
		}
	}

	public void update() throws IOException {
		receive();
		removeDisconnectedClients();
		if(world != null) {
			lastWorldUpdateTime = Utility.updateWorld(world, lastWorldUpdateTime,
					Protocol.MAX_UPDATE_TIMESTEP, Protocol.TIME_SCALE_NANOSECONDS);
			if(Utility.nanosSince(lastClientsUpdate) > NETWORK_SEND_INTERVAL_NANOSECONDS) {
				//TODO optimize entity packet format and sending (batch entity updates)
				for(Entity e : world) {
					populateEntityUpdate(ENTITY_UPDATE_BUFFER, e);
					broadcastUnreliable(ENTITY_UPDATE_BUFFER, client -> client.status == ClientState.STATUS_IN_GAME);
					if(e instanceof PlayerEntity) {
						broadcastPlayerState(null, (PlayerEntity)e);
					}
				}
			
				for(ClientState client : clients.values()) {
					if(client.status == ClientState.STATUS_IN_GAME) {
						client.sendQueue.add(PING_PACKET); //TODO send ping only if necessary (already timing out)
					}
				}
				lastClientsUpdate = System.nanoTime();
			}	
		} else {
			close();
		}
		sendAccumulated();
	}

	public void removeDisconnectedClients() {
		var iterator = clients.values().iterator();
		while(iterator.hasNext()) {
			ClientState client = iterator.next();
			if(client.isDisconnectState() && client.sendQueue.isEmpty()) {
				iterator.remove();
				if(client.status != ClientState.STATUS_CONNECTION_REJECTED) {
					broadcastAndPrint(formatDisconnectMessage(client));
					if(client.player != null && world != null) {
						world.remove(client.player);
						broadcastRemoveEntity(client.player);
					}
				}
			}
		}
	}
	
	private static byte[] buildManualDisconnect(String reason) {
		byte[] message = reason.getBytes(Protocol.CHARSET);
		byte[] packet = new byte[message.length + 6];
		Bytes.putShort(packet, 0, Protocol.TYPE_SERVER_CLIENT_DISCONNECT);
		Bytes.putInteger(packet, 2, message.length);
		Bytes.copy(message, packet, 6);
		return packet;
	}
	
	private long lastClientsUpdate;

	/** Reuseable entity update buffer **/
	byte[] ENTITY_UPDATE_BUFFER = new byte[22]; //protocol, id, posX, posY, velX, velY
	{
		Bytes.putShort(ENTITY_UPDATE_BUFFER, 0, Protocol.TYPE_SERVER_ENTITY_UPDATE);
	}

	private static final SendPacket PING_PACKET = new SendPacket(Bytes.of(Protocol.TYPE_SERVER_PING), true);

	private static void populateEntityUpdate(byte[] packet, Entity e) {
		Bytes.putInteger(packet, 2, e.getID());
		Bytes.putFloat(packet, 6, e.getPositionX());
		Bytes.putFloat(packet, 10, e.getPositionY());
		Bytes.putFloat(packet, 14, e.getVelocityX());
		Bytes.putFloat(packet, 18, e.getVelocityY());
	}

//	/**
//	 * Keep clients up to date with the latest state. (includes message sending)
//	 */
//	public void updateClients() {
//		if(Utility.nanosSince(lastClientsUpdate) > NETWORK_SEND_INTERVAL_NANOSECONDS) {
//			Collection<SendPacket> updates = buildEntityUpdates(world);
//			for(ClientState client : clients.values()) {
//				if(client.status == ClientState.STATUS_IN_GAME) {
//					client.sendQueue.addAll(updates);
//				}
//				client.sendQueue.add(PING_PACKET);
//				lastClientsUpdate = System.nanoTime();
//			}
//
//			for(Entity e : world) {
//				populateEntityUpdate(ENTITY_UPDATE_BUFFER, e);
//				SendPacket packet = new SendPacket(ENTITY_UPDATE_BUFFER.clone(), false);
//				for(ClientState client : clients.values()) {
//					if(client.status == ClientState.STATUS_IN_GAME) {
//						client.sendQueue.add(packet);
//					}
//				}
//			}
//			broadcastPing();
//			lastClientsUpdate = System.nanoTime();
//		}
//	}
//
//	private static final int ENTITY_UPDATE_SIZE = 20;
//
//	private static Collection<SendPacket> buildEntityUpdates(World world) {
//
//		SendPacket[] updates = new SendPacket[Utility.splitQuantity(
//				Protocol.MAX_MESSAGE_LENGTH/ENTITY_UPDATE_SIZE, world.entities())];
//
//		Iterator<Entity> entities = world.iterator();
//
//		for(int i = 0; i < updates.length - 1; i++) { //for all full packets
//			byte[] update = new byte[Protocol.MAX_MESSAGE_LENGTH];
//			for(int e = 0; e < ) {
//
//			}
//			updates[i] = update;
//		}
//
//		byte[] last = new byte[];
//		for(int e = 0; e < remaining; e++) {
//			populateEntityUpdate(last, 2 + e * ENTITY_UPDATE_SIZE; entities.next());
//		}
//
//		int index = 2;
//		for(Entity e : world) {
//			populateEntityUpdate(update, index, e);
//			index += ENTITY_UPDATE_SIZE;
//		}
//		return Arrays.asList(updates);
//	}
//
//	private static void populateEntityUpdate(byte[] packet, int index, Entity e) {
//		Bytes.putInteger(packet, index + 0, e.getID());
//		Bytes.putFloat(packet, index + 4, e.getPositionX());
//		Bytes.putFloat(packet, index + 8, e.getPositionY());
//		Bytes.putFloat(packet, index + 12, e.getVelocityX());
//		Bytes.putFloat(packet, index + 16, e.getVelocityY());
//	}

	public void broadcastPing() {
		broadcastReliable(Bytes.of(Protocol.TYPE_SERVER_PING));
	}

	public void broadcastConsoleMessage(String message) {
		broadcastUnsafe(Protocol.buildConsoleMessage(message), true);
	}

	private void broadcastAndPrint(String consoleMessage) {
		broadcastReliable(Protocol.buildConsoleMessage(consoleMessage));
		log(consoleMessage);
	}

	private String formatDisconnectMessage(ClientState client) {
		return Utility.formatAddress(client.address) +
				" disconnected (reason: '" + client.disconnectReason + "', " + clients.size() + " players connected)";
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
		Bytes.putShort(message, 0, Protocol.TYPE_CLIENT_PLAYER_STATE);
		Bytes.putInteger(message, 2, player.getID());
		Bytes.putShort(message, 6, Protocol.PlayerState.getState(player, false, false));
		broadcastUnreliable(message, recipient -> !recipient.equals(client));
	}

	public void printDebug(PrintStream out) {
		for(ClientState client : clients.values()) {
			out.println(client);
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
		if(canBreakBlock(client, x, y)) {
			breakBlock(x, y);
			client.lastBlockBreakTime = System.nanoTime();
		}
	}
	
	private void processClientPlaceBlock(ClientState client, ByteBuffer packet) {
		int x = packet.getInt();
		int y = packet.getInt();
		//TODO sync block place cooldowns (System.currentTimeMillis() + cooldown)
		if(!world.getForeground().isValid(x, y))
			throw new ClientBadDataException("client sent invalid block coordinates x=" + x + " y=" + y);
		if(canPlaceBlock(client, x, y)) {
			Block blockType = switch(client.player.selected()) {
				case 1 -> new GrassBlock();
				case 2 -> new DirtBlock();
				default -> null;
			};
			
			if(blockType != null) {
				world.getForeground().place(world, x, y, blockType);
				broadcastPlaceBlock(blockType, x, y);
				client.lastBlockPlaceTime = System.nanoTime();	
			}
		}		
	}

	private boolean canBreakBlock(ClientState client, int x, int y) {
		return Utility.nanosSince(client.lastBlockBreakTime) > Protocol.BLOCK_INTERACT_COOLDOWN_NANOSECONDS && world.getForeground().isBlock(x, y) &&
			   Utility.withinDistance(client.player.getPositionX(), client.player.getPositionY(), x, y, Protocol.BLOCK_BREAK_RANGE);
	}
	
	private boolean canPlaceBlock(ClientState client, int x, int y) {
		return Utility.nanosSince(client.lastBlockPlaceTime) > Protocol.BLOCK_INTERACT_COOLDOWN_NANOSECONDS && !world.getForeground().isBlock(x, y) &&
			   Utility.withinDistance(client.player.getPositionX(), client.player.getPositionY(), x, y, Protocol.BLOCK_BREAK_RANGE);
	}

	public void broadcastRemoveBlock(int x, int y) {
		byte[] packet = new byte[10];
		Bytes.putShort(packet, 0, Protocol.TYPE_SERVER_REMOVE_BLOCK);
		Bytes.putInteger(packet, 2, x);
		Bytes.putInteger(packet, 6, y);
		broadcastUnsafe(packet, true);
	}
	
	private void broadcastPlaceBlock(Block block, int x, int y) {
		byte[] blockData = serialize(block, true);
		byte[] packet = new byte[10 + blockData.length];
		Bytes.putShort(packet, 0, Protocol.TYPE_SERVER_PLACE_BLOCK);
		Bytes.putInteger(packet, 2, x);
		Bytes.putInteger(packet, 6, y);
		Bytes.copy(blockData, packet, 10);
		broadcastUnsafe(packet, true);
	}

	public void breakBlock(int blockX, int blockY) {
		Block block = world.getForeground().destroy(world, blockX, blockY);
		broadcastRemoveBlock(blockX, blockY);
		if(block != null) {
			var drop = new ItemEntity<>(world.nextEntityID(), new BlockItem(block), blockX, blockY);
			drop.setVelocityX(-0.2f + ((float) Math.random() * (0.4f)));
			drop.setVelocityY((float) Math.random() * (0.35f));
			world.add(drop);
			broadcastAddEntity(drop);
		}
	}
	
	private void processClientThrowBomb(ClientState client, ByteBuffer data) {
		BombEntity bomb = new ServerBombEntity(this, world.nextEntityID());
		bomb.setPositionX(client.player.getPositionX());
		bomb.setPositionY(client.player.getPositionY());
		float angle = data.getFloat();
		bomb.setVelocityX((float) (Math.cos(angle) * BOMB_THROW_VELOCITY));
		bomb.setVelocityY((float) (Math.sin(angle) * BOMB_THROW_VELOCITY));
		world.add(bomb);
		broadcastAddEntity(bomb);
	}

	private void sendClientConnectReply(ClientState client) {
		if(world != null) {
			//TODO don't send world size in ack, takes to long to serialize entire world
			for(byte[] packet : buildAcknowledgementWorldPackets(world)) {
				sendUnsafe(client, packet, true);
			}

			PlayerEntity player = new ServerPlayerEntity(world.nextEntityID());
			placePlayer(player, world.getForeground());
			world.add(player);
			client.player = player;
			broadcastAddEntity(player); //send entity to already connected players
			sendPlayerID(client, player); //send id of player entity which was sent in world data
			log(Utility.formatAddress(client.address) + " joined (" + getClientCount() + " player(s) connected)");
		} else {
			byte[] response = new byte[3];
			Bytes.putShort(response, 0, Protocol.TYPE_SERVER_CONNECT_ACKNOWLEDGMENT);
			response[2] = Protocol.CONNECT_STATUS_REJECTED;
			sendReliable(client, response);
			client.status = ClientState.STATUS_CONNECTION_REJECTED;
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

	private static byte[][] buildAcknowledgementWorldPackets(World world) {
		byte[] worldBytes = serialize(world, Protocol.COMPRESS_WORLD_DATA);
		byte[][] packets = Bytes.split(worldBytes, Protocol.MAX_MESSAGE_LENGTH - 2, 2, 1);
		packets[0] = buildConnectAcknowledgement(worldBytes.length);
		for(int i = 1; i < packets.length; i++) {
			Bytes.putShort(packets[i], 0, Protocol.TYPE_SERVER_WORLD_DATA);
		}
		return packets;
	}

	private static byte[] buildConnectAcknowledgement(int worldSize) {
		byte[] head = new byte[7];
		Bytes.putShort(head, 0, Protocol.TYPE_SERVER_CONNECT_ACKNOWLEDGMENT);
		head[2] = Protocol.CONNECT_STATUS_WORLD;
		Bytes.putInteger(head, 3, worldSize);
		return head;
	}

	private static byte[] serialize(Transportable object, boolean compress) {
		byte[] serialized = SerializationProvider.getProvider().serialize(object);
		if(compress)
			serialized = Bytes.compress(serialized);
		return serialized;
	}

	private static void sendPlayerID(ClientState client, PlayerEntity player) {
		byte[] packet = new byte[6];
		Bytes.putShort(packet, 0, Protocol.TYPE_SERVER_PLAYER_ID);
		Bytes.putInteger(packet, 2, player.getID());
		sendUnsafe(client, packet, true);
	}

	public void broadcastAddEntity(Entity e) {
		byte[] entity = SerializationProvider.getProvider().serialize(e);
		boolean compress = entity.length > Protocol.MAX_MESSAGE_LENGTH - 3;
		entity = compress ? Bytes.compress(entity) : entity;
		byte[] packet = new byte[3 + entity.length];
		Bytes.putShort(packet, 0, Protocol.TYPE_SERVER_ADD_ENTITY);
		Bytes.putBoolean(packet, 2, compress);
		Bytes.copy(entity, packet, 3);
		broadcastUnsafe(packet, true);
	}

	public void broadcastRemoveEntity(Entity e) {
		byte[] packet = new byte[2 + 4];
		Bytes.putShort(packet, 0, Protocol.TYPE_SERVER_REMOVE_ENTITY);
		Bytes.putInteger(packet, 2, e.getID());
		broadcastUnsafe(packet, true);
	}

	private void sendResponse(InetSocketAddress recipient, ByteBuffer sendBuffer, int receivedMessageID) throws IOException {
		channel.send(sendBuffer.put(Protocol.RESPONSE_TYPE).putInt(receivedMessageID).flip(), recipient);
		sendBuffer.clear();
	}

	public void broadcastReliable(byte[] data) {
		broadcastUnsafe(data.clone(), true);
	}

	public void broadcastUnreliable(byte[] data) {
		broadcastUnsafe(data.clone(), false);
	}

	private void broadcastUnsafe(byte[] data, boolean reliable) {
		SendPacket packet = new SendPacket(data, reliable);
		for(ClientState client : clients.values()) {
			if(!client.isDisconnectState())
				client.sendQueue.add(packet);
		}
	}

	private void broadcastUnreliable(byte[] data, Predicate<ClientState> sendToClient) {
		SendPacket packet = new SendPacket(data.clone(), false);
		for(ClientState client : clients.values()) {
			if(sendToClient.test(client) && !client.isDisconnectState()) {
				client.sendQueue.add(packet);
			}
		}
	}

	public void sendReliable(ClientState client, byte[] data) {
		sendUnsafe(client, data.clone(), true);
	}

	public void sendUnreliable(ClientState client, byte[] data) {
		sendUnsafe(client, data.clone(), false);
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
			processPacket(sender, receiveBuffer, responseBuffer);
		}
	}

	private ClientState getState(InetSocketAddress address) {
		ClientState state = clients.get(address);
		if(state == null)
			clients.put(address, state = new ClientState(address));
		return state;
	}

	private void processPacket(InetSocketAddress sender, ByteBuffer buffer, ByteBuffer sendBuffer) throws IOException {
		buffer.flip(); //flip to set limit and prepare to read packet data
		if(buffer.limit() >= Protocol.HEADER_SIZE) {
			byte type = buffer.get(); //type of message (RESPONSE, RELIABLE, UNRELIABLE)
			int messageID = buffer.getInt(); //received ID or messageID for ack.
			ClientState state = getState(sender);
			switch(type) {
				case Protocol.RESPONSE_TYPE -> {
					if(state.sendReliableID == messageID) {
						state.ping = Utility.nanosSince(state.sendStartTime);
						state.sendAttempts = 0;
						state.sendReliableID++;
						sendBuffer.clear();
						state.sendQueue.poll(); //remove and continue processing
					} //else drop the response
				}

				case Protocol.RELIABLE_TYPE -> {
					if(messageID == state.receiveReliableID) {
						//if the message is the next one, process it and update last message
						sendResponse(sender, sendBuffer, messageID);
						state.receiveReliableID++;
						onReceive(state, buffer.position(Protocol.HEADER_SIZE));
					} else if(messageID < state.receiveReliableID) { //message already received
						sendResponse(sender, sendBuffer, messageID);
					} //else: message received too early
				}

				case Protocol.UNRELIABLE_TYPE -> {
					if(messageID >= state.receiveUnreliableID) {
						state.receiveUnreliableID = messageID + 1;
						onReceive(state, buffer.position(Protocol.HEADER_SIZE));
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
		setupSendBuffer(sendBuffer, Protocol.RELIABLE_TYPE, client.sendReliableID, packet.data);
		channel.send(sendBuffer, client.address);
		sendBuffer.clear();
	}

	private void sendAccumulated() throws IOException {
		for(ClientState client : clients.values()) {
			var queue = client.sendQueue;
			while(!queue.isEmpty()) {
				if(queue.peek().reliable) {
					if(client.sendAttempts == 0) { //initialize
						client.sendStartTime = System.nanoTime();
						sendReliableImpl(client, queue.peek());
						break; //update to await response
					} else if(client.sendAttempts <= Protocol.RESEND_COUNT) { //check if not timed out
						if(Utility.resendIntervalElapsed(client.sendStartTime, client.sendAttempts)) {
							sendReliableImpl(client, queue.peek());
						}
						break; //update to await response
					} else { //handle timeout
						client.handleTimeout();
						break; //stop processing messages, disconnect the client
					}
				} else {
					setupSendBuffer(sendBuffer, Protocol.UNRELIABLE_TYPE, client.sendUnreliableID++, queue.poll().data);
					channel.send(sendBuffer, client.address);
					sendBuffer.clear();
				}
			}
		}
	}

	private static final class ClientState {
		int receiveReliableID, receiveUnreliableID, sendUnreliableID, sendReliableID;
		int sendAttempts;
		long sendStartTime;
		final InetSocketAddress address;
		final Queue<SendPacket> sendQueue;
		byte status;

		/** Client reliable message round trip time in nanoseconds */
		long ping;
		/** Last block break time in nanoseconds offset */
		long lastBlockBreakTime, lastBlockPlaceTime;
		PlayerEntity player;
		String disconnectReason;

		static final byte
			STATUS_CONNECTED = 0, //after the client acks the connect ack
			STATUS_TIMED_OUT = 1, //if the client doesn't send an ack within ack interval
			STATUS_REMOVED = 2, //if server kicks a player or shuts down
			STATUS_SELF_DISCONNECTED = 3, //if the client manually disconnects
			STATUS_CONNECTION_REJECTED = 4, //if the server rejects the client
			STATUS_IN_GAME = 5; //if the client has received the world and player and notifies server

		static String statusToString(byte status) {
			return switch(status) {
				case ClientState.STATUS_CONNECTED -> "CONNECTED";
				case ClientState.STATUS_TIMED_OUT -> "TIMED_OUT";
				case ClientState.STATUS_REMOVED -> "REMOVED";
				case ClientState.STATUS_SELF_DISCONNECTED -> "DISCONNECTED";
				case ClientState.STATUS_CONNECTION_REJECTED -> "REJECTED";
				case ClientState.STATUS_IN_GAME -> "IN_GAME";
				default -> "ILLEGAL";
			};
		}
		
		boolean isDisconnectState() {
			return switch(status) {
				case ClientState.STATUS_TIMED_OUT, ClientState.STATUS_REMOVED, 
					ClientState.STATUS_SELF_DISCONNECTED, ClientState.STATUS_CONNECTION_REJECTED -> true;
				case ClientState.STATUS_CONNECTED, ClientState.STATUS_IN_GAME -> false;
				default -> false;
			};
		}

		ClientState(InetSocketAddress address) {
			this.address = address;
			status = STATUS_CONNECTED;
			sendQueue = new ArrayDeque<>();
		}

		private void handleTimeout() {
			status = STATUS_TIMED_OUT;
			disconnectReason = "client timed out";
		}
		
		public String formattedName() {
			return Utility.formatAddress(address) + ": " + ClientState.statusToString(status);
		}

		@Override
		public String toString() {
			return "ClientState[" + "address:" + Utility.formatAddress(address)
					+ " rSend:" + sendReliableID + " rReceive:" + receiveReliableID
					+ " ping:" + Utility.formatTime(ping) + " sendBufferSize:" + sendQueue.size() + ']';
		}
	}
}
