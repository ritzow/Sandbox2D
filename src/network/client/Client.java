package network.client;

import game.Lobby;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import network.NetworkController;
import network.message.InvalidMessageException;
import network.message.Message;
import network.message.MessageHandler;
import network.message.Protocol;
import network.message.client.ServerConnectRequest;
import network.message.client.ServerInfoRequest;
import network.message.server.ServerConnectAcknowledgment;
import network.message.server.ServerInfo;
import network.message.server.world.BlockGridChunkMessage;
import network.message.server.world.WorldCreationMessage;
import world.World;
import world.WorldManager;

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
		@Override
		public void handle(ServerInfo message, SocketAddress sender, int messageID) {
			if(requestedInfo) {
				System.out.println(message);
				requestedInfo = false;
			}
		}

		@Override
		public void handle(WorldCreationMessage message, SocketAddress sender, int messageID) {
			worldManager = new WorldManager(new World(message.getWidth(), message.getHeight(), message.getGravity()));
		}

		@Override
		public void handle(BlockGridChunkMessage message, SocketAddress sender, int messageID) {
			//TODO implement world transfer. Serialize and compress entire world, then send chunks of the resulting byte array at a time.
		}
	}
	
	protected int lastMessageID = 0;
	
	/**
	 * Note: currently only works after connecting to a server.
	 * @param address
	 */
	public void requestServerInfo(SocketAddress address) {
		requestedInfo = true;
		send(new ServerInfoRequest(), address);
	}
	
	public void send(Message message, SocketAddress address) {
		send(Protocol.construct(++lastMessageID, message, address));
	}
	
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
