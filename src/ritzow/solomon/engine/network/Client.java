package ritzow.solomon.engine.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Arrays;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.world.base.World;
import ritzow.solomon.engine.world.entity.Entity;
import ritzow.solomon.engine.world.entity.PlayerEntity;

public final class Client extends NetworkController {
	
	/** The server's address **/
	protected SocketAddress server;
	
	/** if the server has been successfully connected to **/
	protected volatile boolean connected;
	
	/** messageID **/
	protected volatile int unreliableMessageID, reliableMessageID;
	
	/** The World object sent by the server **/
	protected volatile World world;
	private byte[][] worldPackets;
	protected final Object worldLock;
	
	/** The Player object sent by the server for the client to control **/
	protected PlayerEntity player;
	protected final Object playerLock;
	
	public Client() throws SocketException, UnknownHostException {
		super(new InetSocketAddress(InetAddress.getLocalHost(), 0));
		worldLock = new Object();
		playerLock = new Object();
	}
	
	@Override
	protected void process(SocketAddress sender, int messageID, byte[] data) {
		final short protocol = ByteUtil.getShort(data, 0);
		
		if(sender.equals(server)) {
			switch(protocol) {
				case Protocol.SERVER_CONNECT_ACKNOWLEDGMENT:
					connected = ByteUtil.getBoolean(data, 2);
					synchronized(server)  {
						server.notifyAll();
					}
					break;
				case Protocol.SERVER_WORLD_HEAD:
					worldPackets = new byte[ByteUtil.getInteger(data, 2)][];
					break;
				case Protocol.SERVER_WORLD_DATA:
					processReceiveWorldData(data);
					break;
				case Protocol.SERVER_PLAYER_ENTITY_COMPRESSED:
					processReceivePlayerEntity(data);
					break;
				case Protocol.CONSOLE_MESSAGE:
					System.out.println("[Server] " + new String(data, 2, data.length - 2, Charset.forName("UTF-8")));
					break;
				case Protocol.SERVER_ENTITY_UPDATE:
					processGenericEntityUpdate(data);
					break;
				case Protocol.SERVER_ADD_ENTITY:
					processAddEntity(data, false);
					break;
				case Protocol.SERVER_ADD_ENTITY_COMPRESSED:
					processAddEntity(data, true);
					break;
				case Protocol.SERVER_REMOVE_ENTITY:
					if(world != null) {
						world.remove(ByteUtil.getInteger(data, 2));
					}
					break;
				case Protocol.SERVER_CLIENT_DISCONNECT:
					disconnect(false);
					exit(); //TODO exit just for now
					break;
				default:
					System.err.println("Client received message of unknown protocol " + protocol);
			}
		}
	}
	
	public void send(byte[] data) {
		if(isConnected()) {
			try {
				if(Protocol.isReliable(ByteUtil.getShort(data, 0))) {
					super.sendReliable(server, reliableMessageID++, data, 10, 100);
				} else {
					super.sendUnreliable(server, unreliableMessageID++, data);
				}
			} catch(TimeoutException e) {
				disconnect(false);
			}
		} else {
			throw new ConnectionException("client is not connected to a server");
		}
	}
	
	public boolean isConnected() {
		return server != null && connected;
	}
	
	public SocketAddress getServer() {
		return server;
	}
	
	public World getWorld() {
		if(isConnected()) {
			synchronized(worldLock) {
				while(world == null) {
					try {
						worldLock.wait();
					} catch(InterruptedException e) {
						continue;
					}
				}
				return world;
			}
		} else {
			throw new ConnectionException("client is not connected to a server");
		}
	}
	
	public PlayerEntity getPlayer() {
		if(isConnected()) {
			synchronized(playerLock) {
				while(player == null) {
					try {
						playerLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			return player;
		} else {
			throw new ConnectionException("client is not connected to a server");
		}
	}
	
	public boolean connectTo(SocketAddress address, int timeout) throws IOException {
		if(isSetupComplete() && !isFinished()) {
			if(!isConnected()) {
				server = address;
				byte[] packet = new byte[2];
				ByteUtil.putShort(packet, 0, Protocol.CLIENT_CONNECT_REQUEST);
				try {
					super.sendReliable(address, reliableMessageID++, packet, Math.max(1, timeout/100), Math.min(timeout, 100));
					synchronized(server) {
						try {
							server.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					return connected;
				} catch(TimeoutException e) {
					server = null;
					return false;
				}
			} else {
				disconnect(true);
				return connectTo(address, timeout);
			}
		} else {
			throw new RuntimeException("client must be running to connect to a server");
		}
	}
	
	@Override
	public void exit() {
		disconnect(true);
		super.exit();
	}
	
	public void disconnect(boolean notifyServer) {
		if(isConnected()) {
			try {
				if(notifyServer) {
					byte[] packet = new byte[2];
					ByteUtil.putShort(packet, 0, Protocol.CLIENT_DISCONNECT);
					send(packet);
				}
			} finally {
				server = null;
				connected = false;
				unreliableMessageID = 0;
				reliableMessageID = 0;
				worldPackets = null;
				player = null;
				world = null;
			}
		}
	}
	
	private void processReceivePlayerEntity(byte[] data) {
		try {
			this.player = (PlayerEntity)ByteUtil.deserialize(ByteUtil.decompress(data, 2, data.length - 2));
			getWorld().add(player);
			synchronized(playerLock) {
				playerLock.notifyAll();
			}
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
	}
	
	private void processAddEntity(byte[] data, boolean compressed) {
		if(world != null) {
			try {
				world.add((Entity)ByteUtil.deserialize(compressed ? ByteUtil.decompress(Arrays.copyOfRange(data, 2, data.length)) : Arrays.copyOfRange(data, 2, data.length)));
			} catch(ClassCastException | ReflectiveOperationException e) {
				System.err.println("Error while deserializing received entity");
			}
		}
	}
	
	private void processGenericEntityUpdate(byte[] data) {
		if(world != null) {
			int entityID = ByteUtil.getInteger(data, 2);
			for(Entity e : world) {
				if(e.getID() == entityID) {
					synchronized(e) {
						e.setPositionX(ByteUtil.getFloat(data, 6));
						e.setPositionY(ByteUtil.getFloat(data, 10));
						e.setVelocityX(ByteUtil.getFloat(data, 14));
						e.setVelocityY(ByteUtil.getFloat(data, 18));
					}
					break;
				}
			}
		}
	}
	
	private void processReceiveWorldData(byte[] data) {
		synchronized(worldPackets) {
			for(int i = 0; i < worldPackets.length; i++) {
				//find an empty slot to put the received data
				if(worldPackets[i] == null) {
					worldPackets[i] = data;
					
					//received final packet? create the world.
					if(i == worldPackets.length - 1) {
						world = Protocol.reconstructWorld(worldPackets);
						synchronized(worldLock) {
							worldLock.notifyAll(); //notify waitForWorldStart that the world has started
						}
						
						worldPackets = null;
					}
					break;
				}
			}
		}
	}
}
