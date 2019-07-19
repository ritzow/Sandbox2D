package ritzow.sandbox.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import ritzow.sandbox.client.input.InputContext;
import ritzow.sandbox.client.network.Client;
import ritzow.sandbox.client.network.Client.ClientEvent;
import ritzow.sandbox.client.network.Client.Status;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.Utility;

class ServerJoinContext {
	private final Client client;
	
	public ServerJoinContext() throws IOException {
		System.out.print("Connecting to " + Utility.formatAddress(StartClient.serverAddress)
		+ " from " + Utility.formatAddress(StartClient.localAddress) + "... ");
		client = Client.create(StartClient.localAddress, StartClient.serverAddress);
		client.setEventListener(ClientEvent.TIMED_OUT, this::onTimeout);
		client.setEventListener(ClientEvent.EXCEPTION_OCCURRED, this::onException);
	}
	
	public void listenForServer() {
		client.beginConnect();
		GameLoop.setContext(() -> {
			StartClient.display.poll(new InputContext() {});
			client.update(1, this::process);
		});
	}
	
	private void onTimeout() {
		System.out.println("timed out.");
		try {
			abort();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void onException(IOException e) {
		try {
			System.out.println("An exception occurred: " + 
				(e.getMessage() == null ? e.getClass().getName() : e.getMessage()));
			abort();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void abort() throws IOException {
		client.close();
		GameLoop.setContext(StartClient.mainMenu::update);
	}
	
	private void process(short messageType, ByteBuffer data) {
		if(messageType == Protocol.TYPE_SERVER_CONNECT_ACKNOWLEDGMENT) {
			byte response = data.get();
			switch(response) {
				case Protocol.CONNECT_STATUS_REJECTED -> {
					onDisconnected();
					System.out.println("rejected.");
					try {
						abort();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				case Protocol.CONNECT_STATUS_WORLD -> {
					System.out.print("connected.\nReceiving world data... ");
					InWorldContext worldContext = new InWorldContext(client, data.getInt());
					client.setStatus(Status.CONNECTED);
					GameLoop.setContext(worldContext::listenForServer);
				}

				case Protocol.CONNECT_STATUS_LOBBY ->
					throw new UnsupportedOperationException("CONNECT_STATUS_LOBBY not supported");

				default -> throw new UnsupportedOperationException("unknown connect ack type " + response);
			}
		} else { //TODO process will be called multiple times in the same update call, this will happen
			throw new IllegalStateException("Received other message before SERVER_CONNECT_ACKNOWLEDGMENT: " + messageType);
		}
	}
	
	private void onDisconnected() {
		try {
			StartClient.display.resetCursor();
			client.close();
			if(StartClient.display.wasClosed()) {
				StartClient.exit();
			} else {
				GameLoop.setContext(StartClient.mainMenu::update);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
