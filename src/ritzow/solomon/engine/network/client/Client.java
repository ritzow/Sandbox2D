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
import ritzow.solomon.engine.game.Lobby;
import ritzow.solomon.engine.network.NetworkController;
import ritzow.solomon.engine.network.message.MessageProcessor;
import ritzow.solomon.engine.network.message.Protocol;
import ritzow.solomon.engine.world.WorldManager;

public final class Client extends NetworkController {
	protected SocketAddress serverAddress;
	protected WorldManager worldManager;
	protected Lobby lobby;
	
	/** if the client sent a ServerInfoRequest to get server information **/
	protected boolean requestedInfo;
	
	public Client() throws SocketException, UnknownHostException {
		super(new InetSocketAddress(InetAddress.getLocalHost(), 0));
		processor = new ClientMessageHandler();
	}
	
	/** Handles incoming messages **/
	private class ClientMessageHandler implements MessageProcessor {
		public void processMessage(int messageID, short protocol, SocketAddress sender, byte[] data) {
			
		}
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
		if(attempts == 0)
			throw new RuntimeException("Number of connection attempts cannot be zero");
		else if(timeout != 0 && timeout < attempts)
			throw new RuntimeException("Specified connection timeout is too small");
		setTimeout(timeout/attempts);
		byte[] packet = Protocol.constructServerConnectRequest();
		send(packet, serverAddress);
		DatagramPacket response = new DatagramPacket(new byte[7], 7);
		long startTime = System.currentTimeMillis();
		int attemptsRemaining = attempts;
		while(attemptsRemaining >= 0 && (System.currentTimeMillis() - startTime < timeout || timeout == 0)) {
			try {
				socket.receive(response);
				if(response.getSocketAddress().equals(serverAddress)) {
					if(Protocol.deconstructServerConnectResponse(response.getData())) {
						this.serverAddress = response.getSocketAddress();
						return true;
					} else {
						return false;
					}
				} else {
					continue;
				}
			} catch(PortUnreachableException e) {
				return false;
			} catch(SocketTimeoutException e) {
				if(--attemptsRemaining > 0)
					send(packet, serverAddress);
				continue;
			}
		}
		return false;
	}
}
