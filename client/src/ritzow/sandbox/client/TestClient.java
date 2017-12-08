package ritzow.sandbox.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class TestClient {

	public static void main(String[] args) {
		try {
			Client client = new Client();
			client.connectTo(new InetSocketAddress(InetAddress.getLocalHost(), 50000), 1000);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}
