package ritzow.sandbox.server.network;

import static ritzow.sandbox.network.Network.HEADER_SIZE;
import static ritzow.sandbox.network.Network.MAX_PACKET_SIZE;
import static ritzow.sandbox.network.Network.RELIABLE_TYPE;
import static ritzow.sandbox.network.Network.RESPONSE_TYPE;
import static ritzow.sandbox.network.Network.UNRELIABLE_TYPE;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Transportable;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.network.Protocol.PlayerAction;
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

/**
 * The server manages connected game clients, sends game updates, receives client input, and broadcasts information for clients.
 * @author Solomon Ritzow
 *
 */
public class GameServer {
	private final DatagramChannel channel;
	private final ByteBuffer receiveBuffer, responseBuffer;
	//TODO clients need to be automatically removed if they haven't actually connected, or I need a dummy clientstate that is lightweight
	private final Map<InetSocketAddress, ClientState> clients;
	private final ThreadGroup senderGroup;
	private World world;
	
	public static GameServer start(InetSocketAddress bind) throws IOException {
		return new GameServer(Utility.getProtocolFamily(bind.getAddress()), bind);
	}
	
	private GameServer(ProtocolFamily protocol, InetSocketAddress bind) throws IOException {
		channel = DatagramChannel.open(protocol).bind(bind);
		channel.configureBlocking(false);
		clients = new HashMap<>();
		senderGroup = new ThreadGroup(this + " Packet Senders");
		receiveBuffer = ByteBuffer.allocateDirect(Protocol.MAX_MESSAGE_LENGTH);
		responseBuffer = ByteBuffer.allocateDirect(HEADER_SIZE);
	}
	
	public InetSocketAddress getAddress() throws IOException {
		return (InetSocketAddress)channel.getLocalAddress();
	}
	
	public void close() throws IOException {
		channel.close();
	}
	
	public boolean isOpen() {
		return channel.isOpen();
	}
	
	public int getClientCount() {
		return clients.size(); //TODO this is the number of clients that have ever sent a message to the server
	}
	
	public void setCurrentWorld(World world) {
		world.setRemoveEntities(this::sendRemoveEntity);
		this.world = world;
	}
	
	private void process(ClientState client, ByteBuffer packet) {
		try {
			short type = packet.getShort();
			switch(type) {
			case Protocol.CLIENT_CONNECT_REQUEST:
				sendClientConnectReply(client, true);
				break;
			case Protocol.CLIENT_DISCONNECT:
				client.disconnect();
				broadcastAndPrint(getClientDisconnectMessage(client));
				break;
			case Protocol.CLIENT_PLAYER_ACTION:
				processPlayerAction(client, packet);
				break;
			case Protocol.CLIENT_BREAK_BLOCK:
				processClientBreakBlock(client, packet);
				break;
			case Protocol.CLIENT_BOMB_THROW:
				processClientThrowBomb(client, packet);
				break;
			case Protocol.CLIENT_WORLD_BUILT:
				break; //nothing to do here, maybe in the futoure though
			default:
				throw new ClientBadDataException("received unknown protocol " + type);
			}	
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private void broadcastAndPrint(String consoleMessage) throws IOException {
		broadcastReliable(Protocol.buildConsoleMessage(consoleMessage));
		System.out.println(consoleMessage);
	}
	
	private String getClientDisconnectMessage(ClientState client) {
		return Utility.formatAddress(client.address) + " disconnected (" + clients.size() + " players connected)";
	}
	
	private String getForcefulDisconnectMessage(ClientState client, String reason) {
		return Utility.formatAddress(client.address) + " was disconnected ("
				+ (reason != null && reason.length() > 0 ? "reason: " + reason + ", ": "") 
				+ clients.size() + " players connected)";
	}
	
	private void processPlayerAction(ClientState client, ByteBuffer data) throws IOException {
		if(client.player == null)
			throw new ClientBadDataException("client has no associated player to perform an action");
		PlayerAction action = PlayerAction.forCode(data.get());
		boolean enable = data.get() == 1 ? true : false;
		client.player.processAction(action, enable);
		broadcastPlayerAction(client.player, action, enable);
	}
	
	private void broadcastPlayerAction(PlayerEntity player, PlayerAction action, boolean isEnabled) throws IOException {
		byte[] packet = new byte[8];
		Bytes.putShort(packet, 0, Protocol.SERVER_PLAYER_ACTION);
		Bytes.putInteger(packet, 2, player.getID());
		packet[6] = action.getCode();
		Bytes.putBoolean(packet, 7, isEnabled);
		broadcastReliable(packet);
	}
	
	private void processClientBreakBlock(ClientState client, ByteBuffer data) throws IOException {
		int x = data.getInt();
		int y = data.getInt();
		if(!world.getForeground().isValid(x, y))
			throw new ClientBadDataException("client sent bad x and y block coordinates");
		Block block = world.getForeground().get(x, y);
		if(world.getForeground().destroy(world, x, y)) {
			//TODO block data needs to be reset on drop
			var drop = new ItemEntity<BlockItem>(world.nextEntityID(), new BlockItem(block), x, y);
			drop.setVelocityX(-0.2f + ((float) Math.random() * (0.4f)));
			drop.setVelocityY((float) Math.random() * (0.35f));
			world.add(drop);
			sendRemoveBlock(x, y);
			sendAddEntity(drop);
		}
	}
	
	public void sendRemoveBlock(int x, int y) throws IOException {
		byte[] packet = new byte[10];
		Bytes.putShort(packet, 0, Protocol.SERVER_REMOVE_BLOCK);
		Bytes.putInteger(packet, 2, x);
		Bytes.putInteger(packet, 6, y);
		broadcastReliable(packet);
	}
	
	private static final float BOMB_THROW_VELOCITY = 0.8f;
	
	private void processClientThrowBomb(ClientState client, ByteBuffer data) throws IOException {
		BombEntity bomb = new ServerBombEntity(this, world.nextEntityID());
		bomb.setPositionX(client.player.getPositionX());
		bomb.setPositionY(client.player.getPositionY());
		float angle = data.getFloat();
		bomb.setVelocityX((float) (Math.cos(angle) * BOMB_THROW_VELOCITY));
		bomb.setVelocityY((float) (Math.sin(angle) * BOMB_THROW_VELOCITY));
		world.add(bomb);
		sendAddEntity(bomb);
	}
	
	private void sendClientConnectReply(ClientState client, boolean connected) throws IOException {
		if(connected) {
			PlayerEntity player = new ServerPlayerEntity(world.nextEntityID());
			placePlayerInCenter(player, world.getForeground());
			world.add(player);
			sendAddEntity(player); //send entity to already connected players
			
			for(byte[] packet : buildConnectAcknowledgementPacketsWorld(world)) {
				sendReliable(client, packet);
			}
			
			sendPlayerID(player, client); //send id of player entity (which was sent in world data)
			client.player = player;
			System.out.println(Utility.formatAddress(client.address) + " joined (" + getClientCount() + " players connected)");
		} else {
			byte[] response = new byte[3];
			Bytes.putShort(response, 0, Protocol.SERVER_CONNECT_ACKNOWLEDGMENT);
			response[2] = Protocol.CONNECT_STATUS_REJECTED;
			sendReliable(client, response);
		}
	}

	private void sendPlayerID(PlayerEntity player, ClientState client) throws IOException {
		byte[] packet = new byte[6];
		Bytes.putShort(packet, 0, Protocol.SERVER_PLAYER_ID);
		Bytes.putInteger(packet, 2, player.getID());
		sendReliable(client, packet);
	}
	
	public void sendAddEntity(Entity e) throws IOException {
		byte[] entity = SerializationProvider.getProvider().serialize(e);
		boolean compress = entity.length > Protocol.MAX_MESSAGE_LENGTH - 3;
		entity = compress ? Bytes.compress(entity) : entity;
		byte[] packet = new byte[3 + entity.length];
		Bytes.putShort(packet, 0, Protocol.SERVER_ADD_ENTITY);
		Bytes.putBoolean(packet, 2, compress);
		Bytes.copy(entity, packet, 3);
		broadcastReliable(packet);
	}
	
	public void sendRemoveEntity(Entity e) {
		byte[] packet = new byte[2 + 4];
		Bytes.putShort(packet, 0, Protocol.SERVER_REMOVE_ENTITY);
		Bytes.putInteger(packet, 2, e.getID());
		try {
			broadcastReliable(packet);
		} catch (IOException e1) { //TODO uhhh
			e1.printStackTrace();
		}
	}
	
//	private static byte[] buildServerDisconnect(String reason) {
//		byte[] message = reason.getBytes(Protocol.CHARSET);
//		byte[] packet = new byte[message.length + 6];
//		Bytes.putShort(packet, 0, Protocol.SERVER_CLIENT_DISCONNECT);
//		Bytes.putInteger(packet, 2, message.length);
//		Bytes.copy(message, packet, 6);
//		return packet;
//	}
	
	private static void placePlayerInCenter(PlayerEntity player, BlockGrid grid) {
		float posX = grid.getWidth()/2;
		player.setPositionX(posX);
		for(int i = grid.getHeight() - 2; i > 1; i--) {
			if(grid.get(posX, i) == null && grid.get(posX, i + 1) == null && grid.get(posX, i - 1) != null) {
				player.setPositionY(i);
				break;
			}
		}
	}
	
	private static byte[] serialize(Transportable object, boolean compress) {
		byte[] serialized = SerializationProvider.getProvider().serialize(object);
		if(compress)
			serialized = Bytes.compress(serialized);
		return serialized;
	}
	
	private static byte[][] buildConnectAcknowledgementPacketsWorld(World world) {
		//serialize the world for transfer
		byte[] worldBytes = serialize(world, Protocol.COMPRESS_WORLD_DATA);
		
		//build packets
		byte[][] packets = Bytes.split(worldBytes, Protocol.MAX_MESSAGE_LENGTH - 2, 2, 1);
		
		//create the first packet to send, which contains the number of subsequent packets
		byte[] head = new byte[7];
		Bytes.putShort(head, 0, Protocol.SERVER_CONNECT_ACKNOWLEDGMENT);
		head[2] = Protocol.CONNECT_STATUS_WORLD;
		Bytes.putInteger(head, 3, worldBytes.length);
		packets[0] = head;
		
		for(int i = 1; i < packets.length; i++) {
			Bytes.putShort(packets[i], 0, Protocol.SERVER_WORLD_DATA);
		}
	
		return packets;
	}
	
	public void broadcastUnreliable(byte[] data) throws IOException {
		for(ClientState client : clients.values()) {
			sendUnreliable(client, data);
		}
	}
	
	public void broadcastReliable(byte[] data) throws IOException {
		for(ClientState client : clients.values()) {
			sendReliable(client, data);
		}
	}
	
	public void sendReliable(ClientState client, byte[] data) throws IOException { //TODO race conditions, keeping println causes less bugginess (but still broken)
		System.out.println(client.sendQueue.size());
		client.sendQueue.add(new ReliableSendPacket(data));
	}
	
	public void sendUnreliable(ClientState client, byte[] data) throws IOException {
		System.out.println(client.sendQueue.size());
		client.sendQueue.add(new BasicSendPacket(data));
	}
	
	private static final byte[] PING; static {
		PING = new byte[2];
		Bytes.putShort(PING, 0, Protocol.PING);
	}
	
	public void broadcastPing() throws IOException {
		broadcastReliable(PING);
	}
	
	public void receive() throws IOException {
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
		if(sender == null)
			throw new IllegalArgumentException("sender is null");
		buffer.flip(); //flip to set limit and prepare to read packet data
		if(buffer.limit() >= HEADER_SIZE) {
			byte type = buffer.get(); //type of message (RESPONSE, RELIABLE, UNRELIABLE)
			int messageID = buffer.getInt(); //received ID or messageID for ack.
			ClientState state = getState(sender);
			switch(type) {
			case RESPONSE_TYPE:
				synchronized(state.reliableSendLock) {
					if(state.sendReliableID == messageID) {
						state.receivedByClient = true;
						state.reliableSendLock.notifyAll();
					}
				} break; //else drop the response
			case RELIABLE_TYPE:
				if(messageID == state.receiveReliableID) {
					//if the message is the next one, process it and update last message
					sendResponse(sender, sendBuffer, messageID);
					state.receiveReliableID++;
					process(state, buffer.position(HEADER_SIZE).slice());
				} else if(messageID < state.receiveReliableID) { //message already received
					sendResponse(sender, sendBuffer, messageID);
				} break; //else: message received too early
			case UNRELIABLE_TYPE:
				if(messageID >= state.receiveUnreliableID) {
					state.receiveUnreliableID = messageID + 1;
					process(state, buffer.position(HEADER_SIZE).slice());
				} break; //else: message is outdated
			}
		}
		buffer.clear(); //clear to prepare for next receive
	}
	
	private void sendResponse(InetSocketAddress recipient, ByteBuffer sendBuffer, int receivedMessageID) throws IOException {
		channel.send(sendBuffer.put(RESPONSE_TYPE).putInt(receivedMessageID).flip(), recipient);
		sendBuffer.clear();
	}
	
	private final class ClientState {
		int receiveReliableID, receiveUnreliableID, sendReliableID, sendUnreliableID;
		final Thread sendThread;
		final InetSocketAddress address;
		final ByteBuffer sendBuffer;
		final BlockingQueue<BasicSendPacket> sendQueue;
		final Object reliableSendLock;
		volatile boolean receivedByClient;
		volatile boolean exit;
		
		//Game State
		volatile long pingNanos;
		PlayerEntity player;
		
		ClientState(InetSocketAddress address) {
			this.address = address;
			sendBuffer = ByteBuffer.allocateDirect(MAX_PACKET_SIZE);
			sendQueue = new LinkedBlockingQueue<>();
			reliableSendLock = new Object();
			sendThread = new Thread(senderGroup, this::sender, Utility.formatAddress(address) + " Packet Sender");
			sendThread.start();
		}
		
		void disconnect() { //TODO add more
			exit = true;
			//TODO what happens if the client is sending when interrupted, a ClosedByInterruptException will be thrown, dont want that!
			//may need a volatile boolean that is set to true whenever the method can be interrupted and false when sending using channel
			sendThread.interrupt();
			if(player != null)
				world.remove(player);
			clients.remove(address); //TODO need to maintain a separate list of actually connected clients to prevent from being reregistered by a client sending a response or something
			sendQueue.clear();
		}
		
		private void sender() {
			try {
				while(!exit) {
					BasicSendPacket packet = sendQueue.take();
					if(packet instanceof ReliableSendPacket) {
						if(!sendReliable((ReliableSendPacket)packet, Protocol.RESEND_COUNT, Protocol.RESEND_INTERVAL)) {
							disconnect();
							broadcastAndPrint(getForcefulDisconnectMessage(this, "connection timed out"));
							break;
						}
						receivedByClient = false;
						sendReliableID++;
						sendBuffer.clear();
					} else {
						sendUnreliable(packet);
						sendUnreliableID++;
						sendBuffer.clear();
					}
				}
			} catch (InterruptedException | ClosedByInterruptException e) {
				if(exit != true) {
					e.printStackTrace();
				}
			} catch (IOException e) { //most ClosedChannelExceptions shouldn't occur because clients should be gracefull disconnected
				e.printStackTrace();
			}
		}
		
		private boolean sendReliable(ReliableSendPacket packet, int attempts, int resendInterval) throws IOException, InterruptedException {
			long time = System.nanoTime();
			synchronized(reliableSendLock) {
				ByteBuffer buffer = setupBuffer(RELIABLE_TYPE, sendReliableID, packet.data);
				while(attempts > 0 && !packet.received) {
					channel.send(buffer, address);
					sendBuffer.rewind();
					--attempts;
					reliableSendLock.wait(resendInterval); //wait for ack to be received by processReceive
				}
			}
			
			if(receivedByClient)
				pingNanos = Utility.nanosSince(time);
			return receivedByClient;
		}
		
		private ByteBuffer setupBuffer(byte type, int id, byte[] data) {
			return sendBuffer.put(type).putInt(id).put(data).flip();
		}
		
		private void sendUnreliable(BasicSendPacket packet) throws IOException {
			channel.send(setupBuffer(UNRELIABLE_TYPE, sendUnreliableID, packet.data), address);
		}
		
		@Override
		public String toString() {
			return "ClientState[address:" 
					+ Utility.formatAddress(address)
					+ ",rSend:" + sendReliableID
					+ ",rReceive:" + receiveReliableID
					+ ",ping:" + Utility.formatTime(pingNanos) 
					+ ']';
		}
	}
	
	private static class BasicSendPacket { //TODO add status byte (UNRELIABLE, RELIABLE, RELIABLE_RECEIVED) to this and remove ReliableSendPacket
		final byte[] data;
		
		public BasicSendPacket(byte[] data) {
			this.data = data;
		}
		
		public String toString() {
			return "Unreliable[size:" + data.length + "]";
		}
	}
	
	private static class ReliableSendPacket extends BasicSendPacket {
		volatile boolean received;

		public ReliableSendPacket(byte[] data) {
			super(data);
		}
		
		public String toString() {
			return "Reliable[size:" + data.length + "]";
		}
	}
}
