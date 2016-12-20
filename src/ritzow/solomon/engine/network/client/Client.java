package ritzow.solomon.engine.network.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import ritzow.solomon.engine.game.Lobby;
import ritzow.solomon.engine.network.NetworkController;
import ritzow.solomon.engine.network.message.InvalidMessageException;
import ritzow.solomon.engine.network.message.MessageHandler;
import ritzow.solomon.engine.network.message.Protocol;
import ritzow.solomon.engine.network.message.client.ServerConnectRequest;
import ritzow.solomon.engine.network.message.client.ServerInfoRequest;
import ritzow.solomon.engine.network.message.server.ServerConnectAcknowledgment;
import ritzow.solomon.engine.network.message.server.ServerInfo;
import ritzow.solomon.engine.network.message.server.world.WorldCreationMessage;
import ritzow.solomon.engine.network.message.server.world.WorldDataMessage;
import ritzow.solomon.engine.world.WorldManager;

public final class Client extends NetworkController {
	protected SocketAddress serverAddress;
	protected WorldManager worldManager;
	protected Lobby lobby;
	
	/** if the client sent a ServerInfoRequest to get server information **/
	protected boolean requestedInfo;
	
	public Client() throws SocketException, UnknownHostException {
		super(new InetSocketAddress(InetAddress.getLocalHost(), 0));
		messageHandler = new ClientMessageHandler();
	}
	
	protected class ClientMessageHandler implements MessageHandler {
		
		ArrayList<WorldDataMessage> worldData;
		int worldSize;
		
		@Override
		public void handle(ServerInfo message, SocketAddress sender, int messageID) {
			if(requestedInfo) {
				System.out.println(message);
				requestedInfo = false;
			}
		}

		@Override
		public void handle(WorldCreationMessage message, SocketAddress sender, int messageID) {
			if(sender.equals(serverAddress)) {
				if(worldData == null) {
					worldSize = message.getSize();
					worldData = new ArrayList<WorldDataMessage>(100);
				}
			}
		}

		@Override
		public void handle(WorldDataMessage message, SocketAddress sender, int messageID) {
			if(sender.equals(serverAddress)) {
				if(worldData != null) {
//					try {
//						worldData.add(message);
//						if(worldBuffer.size() == worldSize) {
//							World world = (World)ByteUtil.deserialize(worldBuffer.toByteArray());
//							worldManager = new WorldManager(world);
//						}
//					} catch (IOException | ReflectiveOperationException e) {
//						e.printStackTrace();
//					}
				}
			}
		}
	}
	
	/**
	 * Note: currently only works after connecting to a server.
	 * @param address
	 */
	public void requestServerInfo(SocketAddress address) {
		requestedInfo = true;
		send(new ServerInfoRequest(), address);
	}
	
	protected int lastMessageID = 0;
	
//	public void send(Message message, SocketAddress address) {
//		send(Protocol.construct(++lastMessageID, message, address));
//	}
	
	/**
	 * Connect to a specified SocketAddress using a ServerConnectRequest
	 * @param serverAddress the socket address of the server
	 * @param attempts number of times to resend the connection request
	 * @param timeout the total amount of time in milliseconds to wait for the server to respond
	 * @return whether or not the server responded and accepted the client's connection
	 * @throws IOException if the socket throws an IOException
	 */
	public synchronized boolean connectToServer(SocketAddress serverAddress, int attempts, int timeout) throws IOException {
		if(timeout != 0 && timeout < attempts)
			throw new RuntimeException("Specified connection timeout is too small");
		if(attempts == 0)
			throw new RuntimeException("Number of connection attempts cannot be zero");
		socket.setSoTimeout(timeout/attempts);
		DatagramPacket packet = Protocol.construct(0, new ServerConnectRequest(), serverAddress);
		socket.send(packet);
		DatagramPacket response = new DatagramPacket(new byte[10], 10);
		long startTime = System.currentTimeMillis();
		int attemptsRemaining = attempts;
		while(attemptsRemaining >= 0 && (System.currentTimeMillis() - startTime < timeout || timeout == 0)) {
			try {
				socket.receive(response);
				if(response.getSocketAddress().equals(serverAddress)) {
					if(new ServerConnectAcknowledgment(response.getData(), 6).isAccepted()) {
						this.serverAddress = response.getSocketAddress();
						return true;
					} else {
						return false;
					}
				} else {
					continue;
				}
			} catch(InvalidMessageException e) {
				continue;
			} catch(PortUnreachableException e) {
				return false;
			} catch(SocketTimeoutException e) {
				if(--attemptsRemaining > 0)
					socket.send(packet);
				continue;
			}
		}
		return false;
	}
}
