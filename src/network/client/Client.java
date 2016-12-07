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
import network.message.client.ServerConnectRequest;
import network.message.server.ServerConnectAcknowledgment;
import network.message.server.ServerInfo;
import network.message.server.world.BlockGridChunkMessage;
import network.message.server.world.WorldCreationMessage;
import world.WorldManager;

public final class Client extends NetworkController {

	protected SocketAddress serverAddress;
	protected WorldManager world;
	protected Lobby lobby;
	
	public Client() throws SocketException, UnknownHostException {
		super(new InetSocketAddress(InetAddress.getLocalHost(), 0));
		messageHandler = new ClientMessageHandler();
	}
	
	protected class ClientMessageHandler implements MessageHandler {
		@Override
		public void handle(ServerConnectAcknowledgment message, SocketAddress sender) {
			System.out.println(message);
		}

		@Override
		public void handle(ServerInfo message, SocketAddress sender) {
			System.out.println(message);
		}

		@Override
		public void handle(WorldCreationMessage message, SocketAddress sender) {
			System.out.println(message);
		}

		@Override
		public void handle(BlockGridChunkMessage message, SocketAddress sender) {
			System.out.println(message);
		}
	}
	
	public void send(Message message) {
		if(serverAddress == null)
			throw new RuntimeException("Server address is null");
		byte[] msg = message.getBytes();
		send(new DatagramPacket(msg, msg.length, serverAddress));
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
		byte[] request = new ServerConnectRequest().getBytes();
		DatagramPacket packet = new DatagramPacket(request, request.length, serverAddress);
		socket.send(packet);
		DatagramPacket response = new DatagramPacket(new byte[10], 10);
		long startTime = System.currentTimeMillis();
		int attemptsRemaining = attempts;
		while(attemptsRemaining >= 0 && (System.currentTimeMillis() - startTime < timeout || timeout == 0)) {
			try {
				socket.receive(response);
				if(response.getSocketAddress().equals(serverAddress)) {
					if(new ServerConnectAcknowledgment(response).isAccepted()) {
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
