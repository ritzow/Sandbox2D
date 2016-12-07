package network.client;

import java.io.Closeable;
import java.io.IOException;
import network.NetworkController;

public final class Client extends NetworkController {

	protected SocketAddress serverAddress;
	
	
	}
	
	/**
	 * Connect to a specified SocketAddress using a ServerConnectRequest
	 * @param address the socket address of the server
	 * @param attempts number of times to resend the connection request
	 * @param timeout the total amount of time in milliseconds to wait for the server to respond
	 * @return whether or not the server responded and accepted the client's connection
	 * @throws IOException if the socket throws an IOException
	 */
	public synchronized boolean connectToServer(SocketAddress address, int attempts, int timeout) throws IOException {
		if(timeout != 0 && timeout < attempts)
			throw new UnsupportedOperationException("timeout too small");
		socket.setSoTimeout(timeout/attempts);
		DatagramPacket response = new DatagramPacket(new byte[10], 10);
		ServerConnectRequest request = new ServerConnectRequest();
		send(request, address);
		long startTime = System.currentTimeMillis();
		int attemptsRemaining = attempts;
		while(attemptsRemaining >= 0 && (System.currentTimeMillis() - startTime < timeout || timeout == 0)) {
			try {
				socket.receive(response);
				if(response.getSocketAddress().equals(address)) {
					if(new ServerConnectAcknowledgment(response).isAccepted()) {
						serverAddress = response.getSocketAddress();
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
					send(request, address);
				continue;
			}
		}
		return false;
	}
}
