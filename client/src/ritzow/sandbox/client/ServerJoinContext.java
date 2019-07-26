package ritzow.sandbox.client;

import java.io.IOException;
import java.net.PortUnreachableException;
import java.nio.ByteBuffer;
import ritzow.sandbox.client.input.InputContext;
import ritzow.sandbox.client.network.Client;
import ritzow.sandbox.client.network.Client.ClientEvent;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.Protocol;

class ServerJoinContext {
	private final Client client;
	
	public static void start() {
		try {
			new ServerJoinContext();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private ServerJoinContext() throws IOException {
		System.out.print("Connecting to " + NetworkUtility.formatAddress(StartClient.serverAddress)
		+ " from " + NetworkUtility.formatAddress(StartClient.localAddress) + "... ");
		client = Client.create(StartClient.localAddress, StartClient.serverAddress);
		client.setEventListener(ClientEvent.TIMED_OUT, this::onTimeout);
		client.setEventListener(ClientEvent.EXCEPTION_OCCURRED, this::onException);
		client.beginConnect();
		GameLoop.setContext(this::listen);
	}
	
	private void listen() {
		StartClient.display.poll(InputContext.EMPTY_CONTEXT);
		client.update(1, this::process);
	}
	
	private void onTimeout() {
		System.out.println("timed out.");
		returnToMenu();
	}

	private void onException(IOException e) {
		if(e instanceof PortUnreachableException) {
			System.out.println("server port unreachable.");
		} else {
			System.out.println("an exception occurred: " + 
					(e.getMessage() == null ? e.getClass().getName() : e.getMessage()));	
		}
		returnToMenu();
	}
	
	private void returnToMenu() {
		try {
			client.close();
			GameLoop.setContext(StartClient.mainMenu::update);	
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	private void process(short messageType, ByteBuffer data) {
		if(messageType == Protocol.TYPE_SERVER_CONNECT_ACKNOWLEDGMENT) {
			byte response = data.get();
			switch(response) {
				case Protocol.CONNECT_STATUS_REJECTED -> {
					System.out.println("rejected.");
					StartClient.display.resetCursor();
					returnToMenu();
				}

				case Protocol.CONNECT_STATUS_WORLD -> {
					System.out.print("connected.\nReceiving world data... ");
					InWorldContext worldContext = new InWorldContext(client, data.getInt());
					GameLoop.setContext(worldContext::listenForServer);
				}

				case Protocol.CONNECT_STATUS_LOBBY ->
					throw new UnsupportedOperationException("CONNECT_STATUS_LOBBY not supported");

				default -> throw new UnsupportedOperationException("unknown connect ack type " + response);
			}
		} else { //TODO process will be called multiple times in the same update call, this will happen
			throw new IllegalStateException("Received other message before SERVER_CONNECT_ACKNOWLEDGMENT: " 
					+ messageType);
		}
	}
}
