package ritzow.sandbox.client;

import java.io.IOException;
import java.net.BindException;
import java.net.PortUnreachableException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import ritzow.sandbox.client.network.Client;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.Protocol;

import static ritzow.sandbox.client.data.StandardClientOptions.getLocalAddress;
import static ritzow.sandbox.client.data.StandardClientOptions.getServerAddress;
import static ritzow.sandbox.client.util.ClientUtility.log;

class ServerJoinContext {
	private Client client;
	private InWorldContext worldContext;

	public ServerJoinContext() {
		try {
			log().info(() -> "Connecting to " + NetworkUtility.formatAddress(getServerAddress())
				+ " from " + NetworkUtility.formatAddress(getLocalAddress()));
			client = Client.create(getLocalAddress(), getServerAddress());
			client.setOnTimeout(this::onTimeout).setOnException(this::onException).beginConnect();
		} catch(BindException e) {
			log().log(Level.WARNING, "Bind error", e);
			throw new RuntimeException(e);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void close() {
		try {
			client.close();
			client = null;
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean hasFailed() {
		return client == null;
	}

	public void listen() {
		if(worldContext == null) {
			//wait for server response to connect request
			//only process 1 message so other message types don't get eaten up
			//TODO this is still broken, if message isn't immediately received before other types
			client.update(this::process); //client.update(1, this::process);
		} else {
			worldContext.updatePreInGame();
		}
	}

	private void onTimeout() {
		log().warning("Connection timed out.");
		close();
	}

	private void onException(IOException e) {
		if(e instanceof PortUnreachableException) {
			log().info("Server port unreachable.");
		} else {
			log().log(Level.SEVERE, "An IOException occurred while joining server", e);
		}
		close();
	}

	private void process(short messageType, ByteBuffer data) {
		if(messageType == Protocol.TYPE_SERVER_CONNECT_ACKNOWLEDGMENT) {
			byte response = data.get();
			switch(response) {
				case Protocol.CONNECT_STATUS_REJECTED -> {
					log().info("Server rejected connection");
					close();
				}

				case Protocol.CONNECT_STATUS_WORLD -> {
					log().info("Connected to server");
					//worldSize and playerID integers
					worldContext = new InWorldContext(client, data.getInt(), data.getInt());
				}

				case Protocol.CONNECT_STATUS_LOBBY ->
					throw new UnsupportedOperationException("CONNECT_STATUS_LOBBY not supported.");

				default -> throw new UnsupportedOperationException("unknown connect ack type " + response);
			}
		} else {
			throw new UnsupportedOperationException(messageType + " not supported during connect");
		}
	}
}
