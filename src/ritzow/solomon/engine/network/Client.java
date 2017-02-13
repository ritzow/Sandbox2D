package ritzow.solomon.engine.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.world.base.World;
import ritzow.solomon.engine.world.entity.Player;

public final class Client extends NetworkController {
	protected SocketAddress server;
	protected volatile boolean connected;
	protected volatile int unreliableMessageID, reliableMessageID;
	
	private byte[][] worldPackets;
	protected final Object worldLock;
	protected volatile World world;
	
	protected Player player;
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
			if(protocol == Protocol.SERVER_CONNECT_ACKNOWLEDGMENT) {
				connected = ByteUtil.getBoolean(data, 2);
				synchronized(server)  {
					server.notifyAll();
				}
			}
			
			else if(protocol == Protocol.SERVER_WORLD_HEAD) {
				worldPackets = new byte[ByteUtil.getInteger(data, 2)][];
			}
			
			else if(protocol == Protocol.SERVER_WORLD_DATA) {
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
			}
			
			else if(protocol == Protocol.SERVER_PLAYER_ENTITY) {
				try {
					this.player = (Player)ByteUtil.deserialize(ByteUtil.decompress(data, 2, data.length - 2));
					getWorld().add(player);
					synchronized(playerLock) {
						playerLock.notifyAll();
					}
				} catch (ReflectiveOperationException e) {
					e.printStackTrace();
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
			return new World(ByteUtil.decompress(worldBytes));
		} catch(ReflectiveOperationException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	protected void send(byte[] data) {
		if(server == null)
			throw new RuntimeException("client is not currently connected to a server");
		if(Arrays.binarySearch(Protocol.RELIABLE_PROTOCOLS, ByteUtil.getShort(data, 0)) >= 0) {
			super.sendReliable(server, reliableMessageID++, data, 10, 100);
		} else {
			super.sendUnreliable(server, unreliableMessageID++, data);
		}
	}
	
	public World getWorld() {
		synchronized(worldLock) {
			while(world == null) {
				try {
					worldLock.wait();
				} catch(InterruptedException e) {
					continue;
				}
			}
		}
		return world;
	}
	
	public Player getPlayer() {
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
	}
	
	public boolean connectTo(SocketAddress address, int timeout) throws IOException {
		if(isSetupComplete() && !isFinished()) {
			if(server == null) {
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
	
	public void disconnect() {
		byte[] packet = new byte[2];
		ByteUtil.putShort(packet, 0, Protocol.CLIENT_DISCONNECT);
		try {
			send(packet);
		} finally {
			server = null;
			unreliableMessageID = 0;
			reliableMessageID = 0;
			worldPackets = null;
			player = null;
			world = null;
		}
	}
}
