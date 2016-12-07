package main;

import java.io.IOException;
import java.net.SocketAddress;
import network.client.Client;
import network.server.Server;

/**
 * @author Solomon Ritzow
 *
 */
public class NetworkTest {
	public static void main(String[] args) throws IOException {
		Client client = new Client();
		Server server = new Server(100);

		new Thread(server).start();
		new Thread(client).start();
		
		SocketAddress serverAddress = server.getSocketAddress();
		
		if(client.connectToServer(serverAddress, 1, 1000)) {
			System.out.println("Client connected to " + serverAddress);
		} else {
			System.out.println("Client failed to connect to " + serverAddress);
		}
		
		client.exit();
		server.exit();
	}
}
