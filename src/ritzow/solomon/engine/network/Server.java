package ritzow.solomon.engine.network;

import java.net.*;
import java.util.Arrays;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.world.base.World;
import ritzow.solomon.engine.world.base.WorldUpdater;
import ritzow.solomon.engine.world.entity.Player;

public class Server extends NetworkController {
	protected WorldUpdater worldUpdater;
	protected final ClientState[] clients;
	
	public Server() throws SocketException, UnknownHostException {
		this(20);
	}
	
	public Server(int maxClients) throws SocketException, UnknownHostException {
		super(new InetSocketAddress(InetAddress.getLocalHost(), 50000));
		clients = new ClientState[maxClients];
	}
	
	protected void send(byte[] data, ClientState client) {
		if(Arrays.binarySearch(Protocol.RELIABLE_PROTOCOLS, ByteUtil.getShort(data, 0)) >= 0) {
			try {
				super.sendReliable(client.address, client.reliableMessageID++, data, 10, 100);
			} catch(TimeoutException e) {
				removeClient(client); //TODO try to send a disconnect to client?
			}
		} else {
			super.sendUnreliable(client.address, client.unreliableMessageID++, data);
		}
	}
	
	protected void process(SocketAddress sender, int messageID, byte[] data) {
		final short protocol = ByteUtil.getShort(data, 0);
		final ClientState client = forAddress(sender);
		if(client != null) {
			
		}
		
		else if(protocol == Protocol.CLIENT_CONNECT_REQUEST) {
			//determine if client can connect, and send a response
			boolean canConnect = clientsConnected() < clients.length;
			byte[] response = new byte[3];
			ByteUtil.putShort(response, 0, Protocol.SERVER_CONNECT_ACKNOWLEDGMENT);
			ByteUtil.putBoolean(response, 2, canConnect);
			super.sendReliable(sender, 0, response, 10, 100);
			
			//add the client to the server and send the world to the client if there is one
			if(canConnect) {
				ClientState newClient = new ClientState(sender);
				addClient(newClient);
				
				//increment server's id after sending connect response
				newClient.reliableMessageID = 1;
				
				if(worldUpdater != null & !worldUpdater.isFinished()) {
					
					//send the world to the client
					for(byte[] a : constructWorldPackets(worldUpdater.getWorld())) {
						send(a, newClient);
					}
					
					Player player = new Player();
					newClient.player = player;
					
					World world = getWorld();
					player.setPositionX(world.getForeground().getWidth()/2);
					for(int i = world.getForeground().getHeight() - 2; i > 1; i--) {
						if(world.getForeground().get(player.getPositionX(), i) == null && world.getForeground().get(player.getPositionX(), i + 1) == null &&
							world.getForeground().get(player.getPositionX(), i - 1) != null) {
							player.setPositionY(i);
							break;
						}
					} 
					
					//send the player object to the client
					byte[] playerData = ByteUtil.compress(ByteUtil.serialize(player));
					byte[] playerPacket = new byte[2 + playerData.length];
					ByteUtil.putShort(playerPacket, 0, Protocol.SERVER_PLAYER_ENTITY);
					ByteUtil.copy(playerData, playerPacket, 2);
					send(playerPacket, newClient);
					
					world.add(player); //TODO will this cause concurrent mod exception because the world is already running?
				}
			}
		}
	}
	
	//TODO convert world packet construction int generic Transportable byte array splitting
	private static byte[][] constructWorldPackets(World world) {
		int headerSize = 2;
		int dataBytesPerPacket = Protocol.MAX_MESSAGE_LENGTH - headerSize;

		//serialize the world for transfer, no need to use ByteUtil.serialize because we know what we're serializing (until I start subclassing world)
		byte[] worldBytes = ByteUtil.compress(world.getBytes());
		
		//split world data into evenly sized packets and one extra packet if not evenly divisible by max packet size
		int packetCount = (worldBytes.length/dataBytesPerPacket) + (worldBytes.length % dataBytesPerPacket > 0 ? 1 : 0);
		
		//create the array to store all the constructed packets to send, in order
		byte[][] packets = new byte[1 + packetCount][];

		//create the first packet to send, which contains the number of subsequent packets
		byte[] head = new byte[6];
		ByteUtil.putShort(head, 0, Protocol.SERVER_WORLD_HEAD);
		ByteUtil.putInteger(head, 2, packetCount);
		packets[0] = head;
		
		//construct the packets containing the world data, which begin with a standard header and contain chunks of world bytes
		for(int slot = 1, index = 0; slot < packets.length; slot++) {
			int remaining = worldBytes.length - index;
			int dataSize = Math.min(dataBytesPerPacket, remaining);
			byte[] packet = new byte[headerSize + dataSize];
			ByteUtil.putShort(packet, 0, Protocol.SERVER_WORLD_DATA);
			System.arraycopy(worldBytes, index, packet, headerSize, dataSize);
			packets[slot] = packet;
			index += dataSize;
		}
		
		return packets;
	}
	
	@Override
	public void exit() {
		try {
			stopWorld();
		} catch(RuntimeException e) {
			//ignores "no world to stop" exception
		} finally {
			super.exit();
		}
	}
	
	public void startWorld(World world) {
		if(worldUpdater == null || worldUpdater.isFinished())
			new Thread(worldUpdater = new WorldUpdater(world), "Server World Updater").start();
		else
			throw new RuntimeException("A world is already running");
	}
	
	public void stopWorld() {
		if(worldUpdater != null && !worldUpdater.isFinished())
			worldUpdater.exit();
		else
			throw new RuntimeException("There is no world currently running");
	}
	
	public World getWorld() {
		return worldUpdater == null ? null : worldUpdater.getWorld();
	}
	
	protected ClientState forAddress(SocketAddress address) {
		for(ClientState client : clients) {
			if(client != null && client.address.equals(address)) {
				return client;
			}
		}
		return null;
	}
	
	/**
	 * Checks to see if the specified SocketAddress is already connected to the server
	 * @param address the SocketAddress to query
	 * @return whether or not the address specified is present
	 */
	protected boolean addressPresent(SocketAddress address) {
		for(ClientState client : clients) {
			if(client != null && client.address.equals(address)) {
				return true;
			}
		}
		
		return false;
	}

	protected boolean addClient(ClientState client) {
		for(int i = 0; i < clients.length; i++) {
			if(clients[i] == null) {
				clients[i] = client;
				return true;
			}
		}
		return false;
	}

	protected boolean removeClient(ClientState client) {
		for(int i = 0; i < clients.length; i++) {
			if(clients[i] == client) {
				clients[i] = null;
				return true;
			}
		}
		return false;
	}
	
	protected int clientsConnected() {
		int connected = 0;
		for(int i = 0; i < clients.length; i++) {
			if(clients[i] != null) {
				connected++;
			}
		}
		return connected;
	}
}
