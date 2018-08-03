package ritzow.sandbox.server.network;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Transportable;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.network.Protocol.PlayerAction;
import ritzow.sandbox.network.TimeoutException;
import ritzow.sandbox.server.SerializationProvider;
import ritzow.sandbox.server.world.entity.ServerBombEntity;
import ritzow.sandbox.server.world.entity.ServerPlayerEntity;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.entity.BombEntity;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.entity.PlayerEntity;
import ritzow.sandbox.world.item.BlockItem;

//currently uses single-threaded receive processing, queued blocking message sending
/** The server manages connected game clients, sends game updates, receives client input, and broadcasts information for clients. */
public class GameServer {
	private final DatagramChannel channel;
	private final ByteBuffer receiveBuffer, responseBuffer;
	private final Map<InetSocketAddress, ClientState> clients;
	private final ThreadGroup sendThreadGroup;
	private World world;
	
	private static final float BOMB_THROW_VELOCITY = 0.8f;
	
	public static GameServer start(InetSocketAddress bind) throws IOException {
		return new GameServer(Utility.getProtocolFamily(bind.getAddress()), bind);
	}
	
	private GameServer(ProtocolFamily protocol, InetSocketAddress bind) throws IOException {
		channel = DatagramChannel.open(protocol).bind(bind);
		channel.configureBlocking(false);
		clients = new HashMap<>();
		sendThreadGroup = new ThreadGroup(this + " Packet Senders");
		receiveBuffer = ByteBuffer.allocateDirect(Protocol.MAX_MESSAGE_LENGTH);
		responseBuffer = ByteBuffer.allocateDirect(Protocol.HEADER_SIZE);
	}
	
	public InetSocketAddress getAddress() throws IOException {
		return (InetSocketAddress)channel.getLocalAddress();
	}
	
	public void close() throws IOException {
		clients.forEach((address, client) -> {
			client.status = ClientState.STATUS_REMOVED;
			world.remove(client.player);
			client.disconnectReason = "server shutdown";
		});
		removeDisconnectedClients();
		channel.close();
	}
	
	public boolean isOpen() {
		return channel.isOpen();
	}
	
	public InetSocketAddress[] listClients() {
		return clients.values().stream().map(client -> client.address).toArray(count -> new InetSocketAddress[count]);
	}
	
	public int getClientCount() {
		return clients.size();
	}
	
	public void setCurrentWorld(World world) {
		world.setRemoveEntities(this::broadcastRemoveEntity);
		this.world = world;
	}
	
	//TODO disconnect clients who don't connect properly
	private void onReceive(ClientState client, ByteBuffer packet) {
		short type = packet.getShort();
		switch(type) {
		case Protocol.TYPE_CLIENT_CONNECT_REQUEST:
			sendClientConnectReply(client);
			break;
		case Protocol.TYPE_CLIENT_DISCONNECT:
			processClientDisconnect(client);
			break;
		case Protocol.TYPE_CLIENT_PLAYER_ACTION:
			processPlayerAction(client, packet);
			break;
		case Protocol.TYPE_CLIENT_BREAK_BLOCK:
			processClientBreakBlock(client, packet);
			break;
		case Protocol.TYPE_CLIENT_BOMB_THROW:
			processClientThrowBomb(client, packet);
			break;
		case Protocol.TYPE_CLIENT_WORLD_BUILT:
			break; //nothing to do here, maybe in the futoure though
		default:
			throw new ClientBadDataException("received unknown protocol " + type);
		}
	}
	
	private static final long NETWORK_SEND_INTERVAL_NANOSECONDS = Utility.millisToNanos(100);
	
	byte[] update = new byte[2 + 4 + 4 + 4 + 4 + 4]; //protocol, id, posX, posY, velX, velY
	{
		Bytes.putShort(update, 0, Protocol.TYPE_SERVER_ENTITY_UPDATE);	
	}
	
	/**
	 * Update the server's state. (includes message receiving)
	 * @throws IOException if an internal error occurs
	 */
	public void updateServer() throws IOException {
		removeDisconnectedClients();
		receive();		
	}
	
	private long lastClientsUpdate;
	
	/**
	 * Keep clients up to date with the latest state. (includes message sending)
	 */
	public void updateClients() {
		if(Utility.nanosSince(lastClientsUpdate) > NETWORK_SEND_INTERVAL_NANOSECONDS) {
			for(Entity e : world) {
				populateEntityUpdate(update, e);
				broadcastUnreliable(update); //TODO optimize entity packet format and sending (batch entity updates)
			}
			broadcastPing();
			lastClientsUpdate = System.nanoTime();
		}
		sendToClients(); //TODO run message sending on main server thread
	}
	
	/**
	 * Sends queued messages to clients, including resend attempts.
	 */
	private void sendToClients() {
//		for(ClientState client : clients.values()) {
//			
//		}
	}
	
	private static void populateEntityUpdate(byte[] packet, Entity e) {
		Bytes.putInteger(packet, 2, e.getID());
		Bytes.putFloat(packet, 6, e.getPositionX());
		Bytes.putFloat(packet, 10, e.getPositionY());
		Bytes.putFloat(packet, 14, e.getVelocityX());
		Bytes.putFloat(packet, 18, e.getVelocityY());
	}
	
	public void broadcastPing() {
		broadcastReliable(Bytes.of(Protocol.TYPE_SERVER_PING));
	}
	
	public void broadcastConsoleMessage(String message) {
		broadcastUnsafe(Protocol.buildConsoleMessage(message), true);
	}
	
	private void broadcastAndPrint(String consoleMessage) {
		broadcastReliable(Protocol.buildConsoleMessage(consoleMessage));
		System.out.println(consoleMessage);
	}
	
//	private void manualDisconnect(ClientState client, String reason) {
//		client.status = ClientState.STATUS_REMOVED;
//		client.disconnectReason = reason;
//		sendReliable(client, buildManualDisconnect(reason));
//	}
	
	private String getDisconnectMessage(ClientState client) {
		return Utility.formatAddress(client.address) + " disconnected (reason: '" + client.disconnectReason + "', " + clients.size() + " players connected)";
	}
	
//	private static byte[] buildManualDisconnect(String reason) {
//		byte[] message = reason.getBytes(Protocol.CHARSET);
//		byte[] packet = new byte[message.length + 6];
//		Bytes.putShort(packet, 0, Protocol.TYPE_SERVER_CLIENT_DISCONNECT);
//		Bytes.putInteger(packet, 2, message.length);
//		Bytes.copy(message, packet, 6);
//		return packet;
//	}
	
	private void processPlayerAction(ClientState client, ByteBuffer data) {
		if(client.player == null)
			throw new ClientBadDataException("client has no associated player to perform an action");
		if(world.contains(client.player)) {
			PlayerAction action = PlayerAction.forCode(data.get());
			client.player.processAction(action);
			broadcastPlayerAction(client.player, action);
		} //else what is the point of the player performing the action
	}
	
	private void broadcastPlayerAction(PlayerEntity player, PlayerAction action) {
		byte[] packet = new byte[7];
		Bytes.putShort(packet, 0, Protocol.TYPE_SERVER_PLAYER_ACTION);
		Bytes.putInteger(packet, 2, player.getID());
		packet[6] = action.getCode();
		broadcastReliable(packet);
	}
	
	public void printDebug(PrintStream out) {
		for(ClientState client : clients.values()) {
			out.println(client);
		}
	}
	
	private static void processClientDisconnect(ClientState client) {
		client.status = ClientState.STATUS_DISCONNECTED;
		client.disconnectReason = "self disconnect";
	}
	
	public void removeDisconnectedClients() { //TODO need to interrupt sendQueue.take()
		var iterator = clients.values().iterator();
		while(iterator.hasNext()) {
			ClientState client = iterator.next();
			if(client.status != ClientState.STATUS_CONNECTED) { //TODO handle manual disconnect case
				iterator.remove();
				if(client.player != null) {
					world.remove(client.player);
					broadcastRemoveEntity(client.player);
				}
				
				if(client.status != ClientState.STATUS_REJECTED) {
					broadcastAndPrint(getDisconnectMessage(client));	
				}
			}
		}
	}
	
	private void processClientBreakBlock(ClientState client, ByteBuffer data) {
		int x = data.getInt();
		int y = data.getInt();
		if(!world.getForeground().isValid(x, y))
			throw new ClientBadDataException("client sent bad x and y block coordinates");
		if(canBreakBlock(client, x, y)) {
			breakBlock(x, y);
			client.lastBlockBreak = System.nanoTime();
		}
	}
	
	private static boolean canBreakBlock(ClientState client, int x, int y) {
		return Utility.nanosSince(client.lastBlockBreak) > Protocol.BLOCK_BREAK_COOLDOWN_NANOSECONDS &&
			   Utility.withinDistance(client.player.getPositionX(), client.player.getPositionY(), x, y, Protocol.BLOCK_BREAK_RANGE);
	}
	
	public void broadcastRemoveBlock(int x, int y) {
		byte[] packet = new byte[10];
		Bytes.putShort(packet, 0, Protocol.TYPE_SERVER_REMOVE_BLOCK);
		Bytes.putInteger(packet, 2, x);
		Bytes.putInteger(packet, 6, y);
		broadcastUnsafe(packet, true);
	}
	
	public void breakBlock(int blockX, int blockY) {
		Block block = world.getForeground().destroy(world, blockX, blockY);
		broadcastRemoveBlock(blockX, blockY);
		if(block != null) {
			ItemEntity<BlockItem> drop = new ItemEntity<>(world.nextEntityID(), new BlockItem(block), blockX, blockY);
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
			for(byte[] packet : buildAcknowledgementWorldPackets(world)) { //TODO don't send world size in ack, takes to long to serialize
				sendUnsafe(client, packet, true);
			}
			
			PlayerEntity player = new ServerPlayerEntity(world.nextEntityID());
			placePlayer(player, world.getForeground());
			world.add(player);
			client.player = player;
			broadcastAddEntity(player); //send entity to already connected players
			sendPlayerID(client, player); //send id of player entity which was sent in world data
			System.out.println(Utility.formatAddress(client.address) + " joined (" + getClientCount() + " player(s) connected)");
		} else {
			byte[] response = new byte[3];
			Bytes.putShort(response, 0, Protocol.TYPE_SERVER_CONNECT_ACKNOWLEDGMENT);
			response[2] = Protocol.CONNECT_STATUS_REJECTED;
			sendReliable(client, response);
			client.status = ClientState.STATUS_REJECTED;
			client.disconnectReason = "client rejected";
		}
	}
	
	private static void placePlayer(Entity player, BlockGrid grid) {
		float posX = grid.getWidth()/2;
		player.setPositionX(posX);
		for(int blockY = 1; blockY < grid.getHeight(); blockY++) {
			if(grid.isBlock(posX, blockY) && !(grid.isBlock(posX, blockY + 1) || grid.isBlock(posX, blockY + 2))) {
				player.setPositionY(blockY + 1);
				break;
			}
		}
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
	
	private void receive() throws IOException {
		InetSocketAddress sender;
		while((sender = (InetSocketAddress)channel.receive(receiveBuffer)) != null) {
			processPacket(sender, receiveBuffer, responseBuffer);
		}
	}
	
	private ClientState getState(InetSocketAddress address) {
		ClientState state = clients.get(address);
		if(state == null)
			clients.put(address, state = new ClientState(address, sendThreadGroup, channel));
		return state;
	}
	
	private void processPacket(InetSocketAddress sender, ByteBuffer buffer, ByteBuffer sendBuffer) throws IOException {
		if(sender == null)
			throw new IllegalArgumentException("sender is null");
		buffer.flip(); //flip to set limit and prepare to read packet data
		if(buffer.limit() >= Protocol.HEADER_SIZE) {
			byte type = buffer.get(); //type of message (RESPONSE, RELIABLE, UNRELIABLE)
			int messageID = buffer.getInt(); //received ID or messageID for ack.
			ClientState state = getState(sender);
			switch(type) {
			case Protocol.RESPONSE_TYPE:
				if(state.sendReliableID == messageID) {
					state.receivedByClient = true;
					Utility.notify(state.reliableSendLock);
				} break; //else drop the response
			case Protocol.RELIABLE_TYPE:
				if(messageID == state.receiveReliableID) {
					//if the message is the next one, process it and update last message
					sendResponse(sender, sendBuffer, messageID);
					state.receiveReliableID++;
					onReceive(state, buffer.position(Protocol.HEADER_SIZE).slice());
				} else if(messageID < state.receiveReliableID) { //message already received
					sendResponse(sender, sendBuffer, messageID);
				} break; //else: message received too early
			case Protocol.UNRELIABLE_TYPE:
				if(messageID >= state.receiveUnreliableID) {
					state.receiveUnreliableID = messageID + 1;
					onReceive(state, buffer.position(Protocol.HEADER_SIZE).slice());
				} break; //else: message is outdated
			}
		}
		buffer.clear(); //clear to prepare for next receive
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
			client.sendQueue.add(packet);
		}
	}
	
	public void sendReliable(ClientState client, byte[] data) {
		sendUnsafe(client, data.clone(), true);
	}
	
	//TODO take send channel as a parameter
	public void sendUnreliable(ClientState client, byte[] data) {
		sendUnsafe(client, data.clone(), false);
	}
	
	private static void sendUnsafe(ClientState client, byte[] data, boolean reliable) {
		client.sendQueue.add(new SendPacket(data, reliable));
	}
	
	//TODO convert sending to single threaded operation using interval checking (like for entity send interval)
//	private static enum SendChannels {
//		ENTITY_DATA,
//		PING
//	}
	
	private static final class ClientState {
		int receiveReliableID, receiveUnreliableID, sendUnreliableID; //only accessed by one thread
		volatile int sendReliableID; //accessed by both threads
		final Thread sendThread;
		final InetSocketAddress address;
		final DatagramChannel channel;
		final BlockingQueue<SendPacket> sendQueue;
		final Object reliableSendLock;
		volatile boolean receivedByClient;
		volatile byte status;
		
		private static final byte 
			STATUS_CONNECTED = 0, 
			STATUS_TIMED_OUT = 1, 
			STATUS_REMOVED = 2, 
			STATUS_DISCONNECTED = 3, 
			STATUS_REJECTED = 4;
		
		volatile long pingNanos;
		PlayerEntity player;
		volatile String disconnectReason;
		long lastBlockBreak;
		
		ClientState(InetSocketAddress address, ThreadGroup sendGroup, DatagramChannel channel) {
			this.address = address;
			this.channel = channel;
			status = STATUS_CONNECTED;
			sendQueue = new LinkedBlockingQueue<>();
			reliableSendLock = new Object();
			sendThread = new Thread(sendGroup, this::sender, Utility.formatAddress(address) + " Packet Sender");
			sendThread.start();
		}
		
		private void sender() { //TODO create channel system so that reliable messages dont block unrelated messages
			try {
				ByteBuffer sendBuffer = ByteBuffer.allocateDirect(Protocol.MAX_PACKET_SIZE);
				while(status == STATUS_CONNECTED) {
					SendPacket packet = sendQueue.take(); //TODO deal with blocking when disconnecting
					sendNext(sendBuffer, packet);
				}
			} catch (TimeoutException e) {
				if(status == ClientState.STATUS_CONNECTED) {
					status = STATUS_TIMED_OUT;
					disconnectReason = "client timed out";	
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch(IOException e) {
				e.printStackTrace();
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
					channel.send(sendBuffer, address);
					sendBuffer.rewind();
					synchronized(reliableSendLock) {
						reliableSendLock.wait(Protocol.RESEND_INTERVAL);
					}
					attempts++;
				} while(!receivedByClient && attempts < Protocol.RESEND_COUNT);
				
				if(receivedByClient) {
					pingNanos = Utility.nanosSince(startTime);
					receivedByClient = false;
					sendReliableID++;
				} else {
					throw new TimeoutException("reliable message " + packet + " , id: " + sendReliableID + " timed out.");
				}
			} else {
				setupSendBuffer(sendBuffer, Protocol.UNRELIABLE_TYPE, sendUnreliableID++, packet.data);
				channel.send(sendBuffer, address);
			}
			sendBuffer.clear();
		}
		
		@Override
		public String toString() {
			return "ClientState[" + "address:" + Utility.formatAddress(address)
					+ " rSend:" + sendReliableID + " rReceive:" + receiveReliableID
					+ " ping:" + Utility.formatTime(pingNanos) + " sendBufferSize:" + sendQueue.size() + ']';
		}
	}
	
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
}
