package ritzow.sandbox.server.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Stream;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Transportable;
import ritzow.sandbox.network.NetworkUtility;
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

public class GameServer {
	//TODO server needs to drop unreliable packets if it receives too many (ie client sending at 120 fps while server is 60 fps) ie rate limiting
	//TODO implement encryption on client and server https://howtodoinjava.com/security/java-aes-encryption-example/
	private static final long NETWORK_SEND_INTERVAL_NANOSECONDS = Utility.millisToNanos(200);
	private static final long PLAYER_STATE_BROADCAST_INTERVAL = Utility.millisToNanos(500);
	private static final float BLOCK_DROP_VELOCITY = Utility.convertPerSecondToPerNano(7f);

	private final Server<ClientState> server;
	private World world;
	private long lastWorldUpdateTime;
	private boolean shutdown;

	public GameServer(InetSocketAddress bind) throws IOException {
		this.server = new Server<>(bind);
	}

	private static void log(String message) {
		System.out.println(Utility.formatCurrentTime() + " " + message);
	}

	/** Starts the shutdown process of kicking all the clients off the server **/
	public void shutdown() {
		shutdown = true;
		for(ClientState client : server.clients()) {
			kickClient(client, "Server shutting down");
		}
	}

	public Stream<String> clientNames() {
		return server.clients().stream().map(ClientState::formattedName);
	}

	public void setCurrentWorld(World world) {
		world.setRemoveEntities(this::broadcastRemoveEntity);
		this.world = world;
		this.lastWorldUpdateTime = System.nanoTime();
	}

	public InetSocketAddress getAddress() throws IOException {
		return server.getAddress();
	}

	public int getClientCount() {
		return server.clients().size();
	}

	public boolean isOpen() {
		return server.isOpen();
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
		client.send(packet, reliable);
	}

	private void handleReceive(ClientState client, ByteBuffer packet) {
		try {
			onReceive(packet.getShort(), client, packet);
		} catch(ClientBadDataException e) {
			kickClient(client, "Received bad data - " + e.getMessage());
		}
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

			default -> sendDisconnect(client, "already disconnected", false);
		}
	}

	private long lastClientsUpdate;
	private static final int
		ENTITY_UPDATE_HEADER_SIZE = 6,
		BYTES_PER_ENTITY = 20,
		MAX_ENTITIES_PER_PACKET = (MAX_MESSAGE_LENGTH - ENTITY_UPDATE_HEADER_SIZE)/BYTES_PER_ENTITY;

	public void update() throws IOException {
		server.receive(ClientState::new, this::handleReceive);
		handleClientStatus();
		if(shutdown) {
			if(!server.clients().isEmpty()) {
				server.sendQueued();
			} else {
				server.close();
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
			server.sendQueued();
		}
	}

	//TODO deal with limbo states such as when client responds, but isn't actually doing anything
	private void handleClientStatus() { //TODO this is not allowing disconnect messages to be sent!
		var iterator = server.clients().iterator();
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
						broadcastUnsafe(buildPlayerStateMessage(client.player), false, ClientState::inGame);
					}
				}

				case STATUS_KICKED, STATUS_LEAVE, STATUS_REJECTED -> {
					if(!client.hasPending()) {
						iterator.remove();
						handleDisconnect(client);
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
				"', " + getClientCount() +
				" players connected)"
		);

		if(client.player != null && world.contains(client.player)) {
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
		broadcastUnsafe(buildConsoleMessage(message), true, clientState -> switch (clientState.status) {
			case STATUS_CONNECTED, STATUS_IN_GAME -> true;
			default -> false;
		});
	}

	private void broadcastAndPrint(String consoleMessage) {
		broadcastConsoleMessage(consoleMessage);
		log(consoleMessage);
	}

	private void processClientWorldBuilt(ClientState client) {
		client.status = STATUS_IN_GAME;
		client.sendRecorded();
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
			broadcastUnsafe(buildPlayerStateMessage(client.player), false, r -> r.inGame() && !r.equals(client));
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
		if(server.clients().isEmpty()) {
			return "No connected clients.";
		} else {
			StringJoiner joiner = new StringJoiner("\n");
			for(ClientState client : server.clients()) {
				joiner.add(client.toString());
			}
			return joiner.toString();
		}
	}

	private void processClientBreakBlock(ClientState client, ByteBuffer data) {
		int x = data.getInt();
		int y = data.getInt();
		BlockGrid blocks = world.getBlocks();
		if(!blocks.isValid(x, y))
			throw new ClientBadDataException("client sent invalid block coordinates x=" + x + " y=" + y);
		if(Utility.canBreak(client.player, client.lastBlockBreakTime, world, x, y)) {
			//TODO sync block break cooldowns (System.currentTimeMillis() + cooldown)
			int layer = Utility.getBlockBreakLayer(blocks, x, y);
			if(layer >= 0) {
				sendBreakCooldown(client, System.currentTimeMillis());
				Block block = world.getBlocks().destroy(world, layer, x, y);
				broadcastRemoveBlock(x, y);
				if(block != null) {
					var drop = new ItemEntity<>(world.nextEntityID(), new BlockItem(block), x, y);
					float angle = Utility.random(0, Math.PI);
					drop.setVelocityX((float)Math.cos(angle) * BLOCK_DROP_VELOCITY);
					drop.setVelocityY((float)Math.sin(angle) * BLOCK_DROP_VELOCITY);
					world.add(drop);
					broadcastAddEntity(drop);
				}

				client.lastBlockBreakTime = System.nanoTime();
			}
		}
	}

	private static void sendBreakCooldown(ClientState client, long currentTimeMillis) {
		byte[] packet = new byte[2 + 8];
		Bytes.putShort(packet, 0, TYPE_SERVER_CLIENT_BREAK_BLOCK_COOLDOWN);
		Bytes.putLong(packet, 2, currentTimeMillis);
		client.send(packet.clone(), true);
	}

	private void processClientPlaceBlock(ClientState client, ByteBuffer packet) {
		int x = packet.getInt();
		int y = packet.getInt();
		PlayerEntity player = client.player;
		//TODO sync block place cooldowns (System.currentTimeMillis() + cooldown)
		if(!world.getBlocks().isValid(x, y))
			throw new ClientBadDataException("client sent invalid block coordinates x=" + x + " y=" + y);
		if(Utility.canPlace(player, client.lastBlockPlaceTime, world, x, y)) {
			Block blockType = switch(player.selected()) {
				case SLOT_BREAK -> null;
				case SLOT_PLACE_GRASS -> GrassBlock.INSTANCE;
				case SLOT_PLACE_DIRT -> DirtBlock.INSTANCE;
				default -> throw new ClientBadDataException("invalid item slot " + player.selected());
			};

			if(blockType != null) {
				//TODO there is redundancy here with Utility.canPlace
				int layer = Utility.getBlockPlaceLayer(world.getBlocks(), x, y);
				if(layer >= 0 && (layer < world.getBlocks().getLayers() - 1 || world.getBlocks().isSolidBlockAdjacent(layer, x, y))) {
					world.getBlocks().place(world, layer, x, y, blockType);
					broadcastPlaceBlock(blockType, x, y);
					client.lastBlockPlaceTime = System.nanoTime();
				}
			}
		}
	}

	public void broadcastRemoveBlock(int x, int y) {
		byte[] packet = new byte[10];
		Bytes.putShort(packet, 0, TYPE_SERVER_REMOVE_BLOCK);
		Bytes.putInteger(packet, 2, x);
		Bytes.putInteger(packet, 6, y);
		for(ClientState client : server.clients()) {
			switch(client.status) {
				case STATUS_CONNECTED -> client.recordedSend.add(packet);
				case STATUS_IN_GAME -> client.send(packet, true);
			}
		}
	}

	private void broadcastPlaceBlock(Block block, int x, int y) {
		byte[] blockData = serialize(block, false);
		byte[] packet = new byte[10 + blockData.length];
		Bytes.putShort(packet, 0, TYPE_SERVER_PLACE_BLOCK);
		Bytes.putInteger(packet, 2, x);
		Bytes.putInteger(packet, 6, y);
		Bytes.copy(blockData, packet, 10);
		for(ClientState client : server.clients()) {
			switch(client.status) {
				case STATUS_CONNECTED -> client.recordedSend.add(packet);
				case STATUS_IN_GAME -> client.send(packet, true);
			}
		}
	}

	private void processClientConnectRequest(ClientState client) {
		if(world != null) {
			ServerPlayerEntity player = new ServerPlayerEntity(world.nextEntityID());
			placePlayer(player, world.getBlocks());
			world.add(player);
			client.player = player;
			//send entity to already connected players
			broadcastUnsafe(buildAddEntity(player), true, ClientState::inGame);

			log("Serializing world to send to " + client.formattedName());
			//TODO don't send world size in ack, takes too long to serialize entire world
			for(byte[] packet : buildAcknowledgementPackets(world, player.getID())) {
				client.send(packet, true);
			}
			log("World serialized and sent to " + client.formattedName());
		} else {
			byte[] response = new byte[3];
			Bytes.putShort(response, 0, TYPE_SERVER_CONNECT_ACKNOWLEDGMENT);
			response[2] = CONNECT_STATUS_REJECTED;
			client.send(response, true);
			client.status = STATUS_REJECTED;
			client.disconnectReason = "client rejected";
		}
	}

	private static void placePlayer(Entity player, BlockGrid grid) {
		float posX = grid.getWidth()/2f;
		player.setPositionX(posX);
		for(int blockY = grid.getHeight() - 1; blockY > 0; blockY--) {
			if(grid.isBlockAtLayer(World.LAYER_MAIN, posX, blockY)) {
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
		byte[] message = buildAddEntity(e);
		for(ClientState client : server.clients()) {
			switch(client.status) {
				case STATUS_CONNECTED -> client.recordedSend.add(message);
				case STATUS_IN_GAME -> client.send(message, true);
			}
		}
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
		for(ClientState client : server.clients()) {
			switch(client.status) {
				case STATUS_CONNECTED -> client.recordedSend.add(packet);
				case STATUS_IN_GAME -> client.send(packet, true);
			}
		}
	}

	private void broadcastUnsafe(byte[] data, boolean reliable, Predicate<ClientState> sendToClient) {
		for(ClientState client : server.clients()) {
			if(sendToClient.test(client)) {
				client.send(data, reliable);
			}
		}
	}
}
