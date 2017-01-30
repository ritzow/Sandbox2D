package ritzow.solomon.engine.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.world.World;
import ritzow.solomon.engine.world.WorldUpdater;

public final class Client extends NetworkController {
	protected SocketAddress server;
	protected volatile boolean connected;
	
	protected WorldUpdater worldUpdater;
	protected final Object worldLock;
	
	protected volatile int reliableMessageID, unreliableMessageID;
	
	public Client() throws SocketException, UnknownHostException {
		super(new InetSocketAddress(InetAddress.getLocalHost(), 0), Protocol.getReliableProtocols());
		worldLock = new Object();
	}
	
	private byte[][] worldPackets;
	
	@Override
	protected void process(int messageID, short protocol, SocketAddress sender, byte[] data) {
		if(sender.equals(server)) {
			if(protocol == Protocol.SERVER_CONNECT_ACKNOWLEDGMENT) {
				if(server != null) {
					connected = ByteUtil.getBoolean(data, 0);
					synchronized(server) {
						server.notifyAll();
					}
				}
			}
			
			else if(protocol == Protocol.WORLD_HEAD) {
				worldPackets = new byte[ByteUtil.getInteger(data, 0)][];
			}
			
			else if(protocol == Protocol.WORLD_DATA) {
				synchronized(worldPackets) {
					for(int i = 0; i < worldPackets.length; i++) {
						if(worldPackets[i] == null) {
							worldPackets[i] = data;
							if(i == worldPackets.length - 1) { //received final packet, create the world
								World world = Protocol.deconstructWorldPackets(worldPackets);
								
								if(world == null) {
									worldPackets = null;
									//TODO ask for server to resend world
								} else {
									new Thread(worldUpdater = new WorldUpdater(world), "Client World Updater").start();
									synchronized(worldLock) {
										worldLock.notifyAll(); //notify waitForWorldStart that the world has started
									}
								}
							}
							break;
						}
					}
				}
			}
		}
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
	
	public void waitForWorldStart() {
		if(worldUpdater != null && !worldUpdater.isFinished())
			return;
		synchronized(worldLock) {
			try {
				worldLock.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected void startWorld(World world) {
		if(worldUpdater == null || worldUpdater.isFinished())
			new Thread(worldUpdater = new WorldUpdater(world), "Server World Updater").start();
		else
			throw new RuntimeException("A world is already running");
	}
	
	protected void stopWorld() {
		if(worldUpdater != null && !worldUpdater.isFinished())
			worldUpdater.exit();
		else
			throw new RuntimeException("There is no world currently running");
	}
	
	public World getWorld() {
		return worldUpdater == null ? null : worldUpdater.getWorld();
	}
	
	public boolean connectTo(SocketAddress address, int timeout) throws IOException {
		if(this.server == null) {
			if(this.isSetupComplete() && !this.isFinished()) {
				server = address;
				synchronized(server) {
					reliableSend(Protocol.constructServerConnectRequest(reliableMessageID++), address);
					try {
						server.wait(timeout);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return connected;
				}
			} else {
				throw new RuntimeException("the client must be running to connect to a server");
			}
			
		} else {
			throw new RuntimeException("the client is already connecting to or connected to a server");
		}
	}
}
