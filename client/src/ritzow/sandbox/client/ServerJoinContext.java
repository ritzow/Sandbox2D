package ritzow.sandbox.client;

import java.io.IOException;
import java.net.BindException;
import java.net.PortUnreachableException;
import java.nio.ByteBuffer;
import ritzow.sandbox.client.input.InputContext;
import ritzow.sandbox.client.network.Client;
import ritzow.sandbox.client.network.Client.ClientEvent;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.Protocol;

import static ritzow.sandbox.client.data.StandardClientOptions.*;

class ServerJoinContext {
	private final GameState state;
	private final Client client;
	
	public static void start(GameState state) throws IOException {
		try {
			System.out.print("Connecting to " + NetworkUtility.formatAddress(LAST_SERVER_ADDRESS)
			+ " from " + NetworkUtility.formatAddress(LAST_LOCAL_ADDRESS) + "... ");
			Client client = Client.create(LAST_LOCAL_ADDRESS, LAST_SERVER_ADDRESS);
			ServerJoinContext context = new ServerJoinContext(state, client);
			client.setEventListener(ClientEvent.TIMED_OUT, context::onTimeout)
				.setEventListener(ClientEvent.EXCEPTION_OCCURRED, context::onException)
				.beginConnect();
			GameLoop.setContext(context::listen);	
		} catch(BindException e) {
			System.out.println("bind error: " + e.getMessage() + ".");
			GameLoop.setContext(state.menuContext::update);	
		}
	}
	
	private ServerJoinContext(GameState state, Client client) {
		this.state = state;
		this.client = client;
	}
	
	private void listen() {
		state.display.poll(InputContext.EMPTY_CONTEXT);
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
			GameLoop.setContext(state.menuContext::update);	
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void process(short messageType, ByteBuffer data) {
		if(messageType == Protocol.TYPE_SERVER_CONNECT_ACKNOWLEDGMENT) {
			byte response = data.get();
			switch(response) {
				case Protocol.CONNECT_STATUS_REJECTED -> {
					System.out.println("rejected.");
					state.display.resetCursor();
					returnToMenu();
				}

				case Protocol.CONNECT_STATUS_WORLD -> {
					System.out.print("connected.\nReceiving world data... ");
					//worldSize and playerID integers
					InWorldContext worldContext = new InWorldContext(state, client, data.getInt(), data.getInt());
					GameLoop.setContext(worldContext::listenForServer);
				}

				case Protocol.CONNECT_STATUS_LOBBY ->
					throw new UnsupportedOperationException("CONNECT_STATUS_LOBBY not supported.");

				default -> throw new UnsupportedOperationException("unknown connect ack type " + response);
			}
		} else { //TODO process will be called multiple times in the same update call, this will happen
			throw new IllegalStateException("Received other message before SERVER_CONNECT_ACKNOWLEDGMENT: " 
					+ messageType);
		}
	}
}
