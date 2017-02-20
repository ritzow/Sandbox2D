package ritzow.solomon.engine.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.util.Service;
import ritzow.solomon.engine.world.base.World;
import ritzow.solomon.engine.world.base.WorldUpdater;
import ritzow.solomon.engine.world.entity.Entity;
import ritzow.solomon.engine.world.entity.PlayerEntity;

public class Server extends NetworkController {
	protected Service logicProcessor;
	protected WorldUpdater worldUpdater;
	protected final ClientState[] clients;
	protected volatile int lastEntityID;
	
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
				disconnectClient(client);
			}
		} else {
			super.sendUnreliable(client.address, client.unreliableMessageID++, data);
		}
	}
	
	public void broadcast(byte[] data) {
		if(Arrays.binarySearch(Protocol.RELIABLE_PROTOCOLS, ByteUtil.getShort(data, 0)) >= 0) {
			for(ClientState client : clients) {
				if(client != null) {
					try {
						super.sendReliable(client.address, client.reliableMessageID++, data, 10, 100);
					} catch(TimeoutException e) {
						disconnectClient(client);
					}
				}
			}
		} else {
			for(ClientState client : clients) {
				if(client != null) {
					super.sendUnreliable(client.address, client.unreliableMessageID++, data);
				}
			}
		}
	}
	
	protected final void process(SocketAddress sender, int messageID, byte[] data) {
		final short protocol = ByteUtil.getShort(data, 0);
		final ClientState client = forAddress(sender);
		
		if(client != null) {
			switch(protocol) {
			case Protocol.CLIENT_DISCONNECT:
				disconnectClient(client);
				break;
			case Protocol.CONSOLE_MESSAGE:
				byte[] decoration = ('<' + client.username + "> ").getBytes();
				byte[] broadcast = new byte[2 + decoration.length + data.length - 2];
				ByteUtil.putShort(broadcast, 0, Protocol.CONSOLE_MESSAGE);
				ByteUtil.copy(decoration, broadcast, 2);
				System.arraycopy(data, 2, broadcast, 2 + decoration.length, data.length - 2);
				broadcast(broadcast);
				break;
			case Protocol.CLIENT_PLAYER_ACTION:
				if(client.player != null) {
					Protocol.PlayerAction.processPlayerMovementAction(data, client.player);
				}
				break;
			default:
				System.out.println("unknown protocol");
			}
		}
		
		else if(protocol == Protocol.CLIENT_CONNECT_REQUEST) {
			//determine if client can connect, and send a response
			boolean canConnect = clientsConnected() < clients.length;
			byte[] response = new byte[3];
			ByteUtil.putShort(response, 0, Protocol.SERVER_CONNECT_ACKNOWLEDGMENT);
			ByteUtil.putBoolean(response, 2, canConnect);
			super.sendReliable(sender, 0, response, 10, 100);
			
			if(canConnect) {
				ClientState newClient = new ClientState(1, sender);
				
				if(worldUpdater != null && !worldUpdater.isFinished()) {
					//send the world to the client
					for(byte[] a : constructWorldPackets(worldUpdater.getWorld())) {
						send(a, newClient);
					}
					
					PlayerEntity player = new PlayerEntity(++lastEntityID);
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
					
					//change the packet to an add entity packet for all other clients
					ByteUtil.putShort(playerPacket, 0, Protocol.SERVER_ADD_ENTITY);
					
					for(ClientState c : clients) {
						if(c != null) {
							send(playerPacket, c);
						}
					}
					
					world.add(player);
				}
				
				addClient(newClient);
			}
		}
	}
	
	private static byte[][] constructWorldPackets(World world) {
		int headerSize = 2;
		int dataBytesPerPacket = Protocol.MAX_MESSAGE_LENGTH - headerSize;

		//serialize the world for transfer, no need to use ByteUtil.serialize because we know what we're serializing (until I start subclassing world)
		byte[] worldBytes = ByteUtil.compress(ByteUtil.serialize(world));
		
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
		if(worldUpdater == null || worldUpdater.isFinished()) {
			new Thread(worldUpdater = new WorldUpdater(world), "Server World Updater").start();
			new Thread(logicProcessor = new WorldLogicProcessor(world), "World Logic Updater").start();
		} else {
			throw new RuntimeException("A world is already running");	
		}
	}
	
	public void stopWorld() {
		if(worldUpdater != null && worldUpdater.isRunning()) {
			worldUpdater.exit();
			if(logicProcessor != null && logicProcessor.isRunning())
			logicProcessor.exit();
		} else {
			throw new RuntimeException("There is no world currently running");
		}
	}
	
	public World getWorld() {
		return worldUpdater == null ? null : worldUpdater.getWorld();
	}
	
	public void disconnectClient(ClientState client) {
		removeSender(client.address);
		if(worldUpdater != null)
			worldUpdater.getWorld().remove(client.player);
		removeClient(client);
	}
	
	protected ClientState forAddress(SocketAddress address) {
		for(ClientState client : clients) {
			if(client != null && client.address.equals(address)) {
				return client;
			}
		}
		return null;
	}
	
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
	
	private static final class ClientState {
		protected volatile int unreliableMessageID;
		protected volatile int reliableMessageID;
		protected volatile PlayerEntity player;
		protected volatile String username;
		protected final SocketAddress address;
		
		public ClientState(int initReliable, SocketAddress address) {
			this.reliableMessageID = initReliable;
			this.address = address;
			username = "anonymous";
		}
		
		public boolean equals(Object o) {
			if(o instanceof ClientState) {
				ClientState c = (ClientState)o;
				return username.equals(c.username) 
						&& address.equals(c.address) 
						&& unreliableMessageID == c.unreliableMessageID 
						&& reliableMessageID == c.reliableMessageID;
			} else {
				return false;
			}
		}
	}
	
	private final class WorldLogicProcessor implements Service {
		private boolean setup, exit, finished;
		private final World world;
		
		public WorldLogicProcessor(World world) {
			this.world = world;
		}

		public synchronized boolean isSetupComplete() {
			return setup;
		}

		public synchronized void exit() {
			exit = true;
			this.notifyAll();
		}

		public synchronized boolean isFinished() {
			return finished;
		}

		public void run() {
			synchronized(this) {
				setup = true;
				this.notifyAll();
			}
			
			while(!exit) {
				try {
					for(Entity e : world) {
						synchronized(e) {
							broadcast(Protocol.buildGenericEntityUpdate(e));
						}
					}
					Thread.sleep(100);
				} catch (Exception e) {
					continue;
				}
			}
			
			synchronized(this) {
				finished = true;
				this.notifyAll();
			}
		}
	}
}
