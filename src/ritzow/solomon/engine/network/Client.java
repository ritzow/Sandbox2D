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
				case Protocol.SERVER_CONNECT_ACKNOWLEDGMENT: {
					connected = ByteUtil.getBoolean(data, 2);
					synchronized(server)  {
						server.notifyAll();
					}
					break;
				}
				case Protocol.SERVER_WORLD_HEAD: {
					worldPackets = new byte[ByteUtil.getInteger(data, 2)][];
					break;
				}
				case Protocol.SERVER_WORLD_DATA: {
					synchronized(worldPackets) {
						for(int i = 0; i < worldPackets.length; i++) {
							//find an empty slot to put the received data
							if(worldPackets[i] == null) {
								worldPackets[i] = data;
								
								//received final packet? create the world.
								if(i == worldPackets.length - 1) {
									world = deconstructWorldPackets(worldPackets);
									synchronized(worldLock) {
										worldLock.notifyAll(); //notify waitForWorldStart that the world has started
									}
									
									worldPackets = null;
								}
								break;
							}
						}
					}
					break;
				}
				case Protocol.SERVER_PLAYER_ENTITY: {
					try {
						this.player = (PlayerEntity)ByteUtil.deserialize(ByteUtil.decompress(data, 2, data.length - 2));
						getWorld().add(player);
						synchronized(playerLock) {
							playerLock.notifyAll();
						}
					} catch (ReflectiveOperationException e) {
						e.printStackTrace();
					}
					break;
				}
				case Protocol.CONSOLE_MESSAGE: {
					System.out.println("[Server] " + new String(data, 2, data.length - 2, Charset.forName("UTF-8")));
					break;
				}
				case Protocol.GENERIC_ENTITY_UPDATE: {
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
					break;
				}
				case Protocol.SERVER_ADD_ENTITY: {
					if(world != null) {
						try {
							world.add((Entity)ByteUtil.deserialize(ByteUtil.decompress(Arrays.copyOfRange(data, 2, data.length))));
						} catch(ClassCastException | ReflectiveOperationException e) {
							System.err.println("Error while deserializing received entity");
						}
					}
				}
				break;
				default: {
					System.err.println("Client received message of unknown protocol " + protocol);
				}
			}
		}
	}
	
	private static World deconstructWorldPackets(byte[][] data) {
		final int headerSize = 2; //WORLD_DATA protocol
		//create a sum of the number of bytes so that a single byte array can be allocated
		int bytes = 0;
		for(byte[] a : data) {
			bytes += (a.length - headerSize);
		}
		
		//the size of the world data (sum of all arrays without 2 byte headers)
		byte[] worldBytes = new byte[bytes];
		
		//copy the received packets into the final world data array
		int index = 0;
		for(byte[] array : data) {
			System.arraycopy(array, headerSize, worldBytes, index, array.length - headerSize);
			index += (array.length - headerSize);
		}
		
		try {
			return (World)ByteUtil.deserialize(ByteUtil.decompress(worldBytes));
		} catch(ReflectiveOperationException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void send(byte[] data) {
		if(isConnected()) {
			try {
				if(Arrays.binarySearch(Protocol.RELIABLE_PROTOCOLS, ByteUtil.getShort(data, 0)) >= 0) {
					super.sendReliable(server, reliableMessageID++, data, 10, 100);
				} else {
					super.sendUnreliable(server, unreliableMessageID++, data);
				}
			} catch(TimeoutException e) {
				disconnect();
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
				disconnect();
				return connectTo(address, timeout);
			}
		} else {
			throw new RuntimeException("client must be running to connect to a server");
		}
	}
	
	public void exit() {
		disconnect();
		super.exit();
	}
	
	public void disconnect() {
		if(isConnected()) {
			byte[] packet = new byte[2];
			ByteUtil.putShort(packet, 0, Protocol.CLIENT_DISCONNECT);
			try {
				send(packet);
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
}
