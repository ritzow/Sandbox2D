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
	protected WorldUpdater worldManager;
	protected boolean connected;
	
	public Client() throws SocketException, UnknownHostException {
		super(new InetSocketAddress(InetAddress.getLocalHost(), 0));
	}
	
	private byte[][] worldPackets;
	
	@Override
	protected void processMessage(int messageID, short protocol, SocketAddress sender, byte[] data) {
		System.out.println("Client received message of ID " + messageID + " and type " + protocol);
		
		if(sender.equals(server)) {
			if(protocol == Protocol.SERVER_CONNECT_ACKNOWLEDGMENT) {
				connected = ByteUtil.getBoolean(data, 0);
				synchronized(server) {
					server.notifyAll(); //notify the connectTo method that the client connected to the server successfully.
				}
			}
			
			else if(protocol == Protocol.WORLD_HEAD) {
				worldPackets = new byte[ByteUtil.getInteger(data, 0)][];
			}
			
			else if(protocol == Protocol.WORLD) {
				for(int i = 0; i < worldPackets.length; i++) {
					if(worldPackets[i] == null) {
						worldPackets[i] = data;
						
						if(i == worldPackets.length - 1) { //received final packet, create the world
							World world = Protocol.deconstructWorldPackets(worldPackets);
							new Thread(worldManager = new WorldUpdater(world), "Client World Manager").start();
						}
						
						break;
					}
				}
			}
		}
	}
	
	public World getWorld() {
		return worldManager == null ? null : worldManager.getWorld();
	}
	
	public boolean connectTo(SocketAddress server, int timeout) throws IOException {
		if(this.server == null) {
			if(this.isSetupComplete() && !this.isFinished()) {
				this.server = server;
				send(Protocol.constructServerConnectRequest(), server);
				synchronized(server) {
					try {
						this.wait(timeout);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				return connected;
			} else {
				throw new RuntimeException("the client must be running to connect to a server");
			}
			
		} else {
			send(Protocol.constructClientDisconnect(0), server); //TODO what about handling timeouts, etc.?
			return connectTo(server, timeout);
		}
	}
	
//	/**
//	 * Connect to a specified SocketAddress using a ServerConnectRequest
//	 * @param serverAddress the socket address of the server
//	 * @param attempts number of times to resend the connection request
//	 * @param timeout the total amount of time in milliseconds to wait for the server to respond
//	 * @return whether or not the server responded and accepted the client's connection
//	 * @throws IOException if the socket throws an IOException
//	 */
//	public synchronized boolean connectToServer(SocketAddress serverAddress, int attempts, int timeout) throws IOException {
//		if(attempts == 0)
//			throw new RuntimeException("Number of connection attempts cannot be zero");
//		else if(timeout != 0 && timeout < attempts)
//			throw new RuntimeException("Specified connection timeout is too small");
//		setTimeout(timeout/attempts);
//		byte[] packet = Protocol.constructServerConnectRequest();
//		DatagramPacket datagram = new DatagramPacket(packet, packet.length, serverAddress);
//		socket.send(datagram);
//		DatagramPacket response = new DatagramPacket(new byte[7], 7);
//		long startTime = System.currentTimeMillis();
//		int attemptsRemaining = attempts;
//		while(attemptsRemaining >= 0 && (System.currentTimeMillis() - startTime < timeout || timeout == 0)) {
//			try {
//				socket.receive(response);
//				if(response.getSocketAddress().equals(serverAddress)) {
//					if(Protocol.deconstructServerConnectResponse(response.getData())) {
//						this.server = response.getSocketAddress();
//						return true;
//					} else {
//						return false;
//					}
//				} else {
//					continue;
//				}
//			} catch(PortUnreachableException e) {
//				return false;
//			} catch(SocketTimeoutException e) {
//				if(--attemptsRemaining > 0)
//					socket.send(datagram);
//				continue;
//			}
//		}
//		return false;
//	}
}
