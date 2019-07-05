package ritzow.sandbox.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.Control.Button;
import ritzow.sandbox.client.input.InputContext;
import ritzow.sandbox.client.input.controller.InteractionController;
import ritzow.sandbox.client.input.controller.TrackingCameraController;
import ritzow.sandbox.client.network.Client;
import ritzow.sandbox.client.network.Client.ClientEvent;
import ritzow.sandbox.client.network.Client.Status;
import ritzow.sandbox.client.network.GameTalker;
import ritzow.sandbox.client.util.SerializationProvider;
import ritzow.sandbox.client.world.ClientWorldRenderer;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.PlayerEntity;

public class InWorldContext implements GameContext, GameTalker {
	Client client;
	ClientWorldRenderer worldRenderer;
	TrackingCameraController cameraGrip;
	InteractionController interactionControls;
	WorldState worldState;
	ClientPlayerEntity player;
	long lastWorldUpdate, lastCameraUpdate;
	
	private static final class WorldState {
		World world;
		byte[] data;
		int worldBytesRemaining;
	}

	private void leaveServer() {
		startDisconnect();
		GameLoop.setContext(() -> client.update(this::process));
	}

	@Override
	public void run() {
		try {
			System.out.print("Connecting to " + Utility.formatAddress(StartClient.serverAddress)
			+ " from " + Utility.formatAddress(StartClient.localAddress) + "... ");
			client = Client.create(StartClient.localAddress, StartClient.serverAddress);
			client.setEventListener(ClientEvent.DISCONNECTED, this::onDisconnected);
			client.setEventListener(ClientEvent.TIMED_OUT, this::onTimeout);
			client.setEventListener(ClientEvent.EXCEPTION_OCCURRED, this::onException);
			client.beginConnect();
			GameLoop.setContext(this::waitForHandshake);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	private static void processServerConsoleMessage(ByteBuffer data) {
		System.out.println("[Server Message] " + new String(getRemainingBytes(data), Protocol.CHARSET));
	}

	private void process(short messageType, ByteBuffer data) {
		if(messageType == Protocol.TYPE_SERVER_CONNECT_ACKNOWLEDGMENT) {
			processServerConnectAcknowledgement(data);
		} else if(client.getStatus() != Status.CONNECTING) {
			switch(messageType) {
				case Protocol.TYPE_CONSOLE_MESSAGE -> processServerConsoleMessage(data);
				case Protocol.TYPE_SERVER_WORLD_DATA -> processReceiveWorldData(data);
				case Protocol.TYPE_SERVER_ENTITY_UPDATE -> processUpdateEntity(data);
				case Protocol.TYPE_SERVER_ADD_ENTITY -> processAddEntity(data);
				case Protocol.TYPE_SERVER_REMOVE_ENTITY -> processRemoveEntity(data);
				case Protocol.TYPE_SERVER_PLAYER_ID -> processReceivePlayerEntityID(data);
				case Protocol.TYPE_SERVER_REMOVE_BLOCK -> processServerRemoveBlock(data);
				case Protocol.TYPE_CLIENT_PLAYER_STATE -> processPlayerState(data);
				case Protocol.TYPE_SERVER_CLIENT_DISCONNECT -> processServerDisconnect(data);
				case Protocol.TYPE_SERVER_PING -> {}
				default -> throw new IllegalArgumentException("Client received message of unknown protocol " + messageType);
			}
		} else {
			throw new IllegalStateException("Received non-TYPE_SERVER_CONNECT_ACKNOWLEDGMENT message before connecting to server");
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Entity> T getEntityFromID(int ID) {
		for(Entity e : worldState.world) { //block until world is received so no NPEs happen
			if(e.getID() == ID) {
				return (T)e;
			}
		}
		throw new IllegalStateException("No entity with ID " + ID + " exists");
	}
	
	private void processServerConnectAcknowledgement(ByteBuffer data) {
		byte response = data.get();
		switch(response) {
			case Protocol.CONNECT_STATUS_REJECTED -> {
				client.setStatus(Status.DISCONNECTED);
				var action = client.getAction(ClientEvent.DISCONNECTED);
				if(action != null) action.run();
				onRejected();
			}

			case Protocol.CONNECT_STATUS_WORLD -> {
				var state = new WorldState();
				int remaining = data.getInt();
				state.worldBytesRemaining = remaining;
				state.data = new byte[remaining];
				this.worldState = state;
				client.setStatus(Status.CONNECTED);
				onAccepted();
			}

			case Protocol.CONNECT_STATUS_LOBBY ->
				throw new UnsupportedOperationException("CONNECT_STATUS_LOBBY not supported");

			default -> throw new UnsupportedOperationException("unknown connect ack type " + response);
		}
	}
	
	/**
	 * Notifies the server, disconnects, and stops the client. May block
	 * while disconnect is occuring.
	 * @throws IOException if an internal I/O socket error occurs
	 * @throws InterruptedException if the client is interrupted while disconnecting
	 */
	public void startDisconnect() {
		client.setStatus(Status.DISCONNECTING);
		client.sendReliable(Bytes.of(Protocol.TYPE_CLIENT_DISCONNECT), () -> {
			client.setStatus(Status.DISCONNECTED);
			var action = client.getAction(ClientEvent.DISCONNECTED);
			if(action != null) action.run();
		});
	}
	
	private void processServerDisconnect(ByteBuffer data) {
		client.setStatus(Status.DISCONNECTED);
		int length = data.getInt();
		byte[] array = new byte[length];
		data.get(array);
		System.out.println("Disconnected from server: " + new String(array, Protocol.CHARSET));
		var action = client.getAction(ClientEvent.DISCONNECTED);
		if(action != null) action.run();
	}
	
	private void processPlayerState(ByteBuffer data) {
		PlayerEntity e = getEntityFromID(data.getInt());
		Protocol.PlayerState.updatePlayer(e, data.get());
	}

	private void processServerRemoveBlock(ByteBuffer data) {
		World world = worldState.world;
		world.getForeground().destroy(world, data.getInt(), data.getInt());
	}

	private void processReceivePlayerEntityID(ByteBuffer data) {
		int id = data.getInt();
		ClientPlayerEntity player = getEntityFromID(id);
		onWorldJoin(worldState.world, player);
	}

	private void processRemoveEntity(ByteBuffer data) {
		int id = data.getInt();
		worldState.world.removeIf(e -> e.getID() == id);
	}

	private static <T> T deserialize(boolean compress, byte[] data) {
		return SerializationProvider.getProvider().deserialize(compress ? Bytes.decompress(data) : data);
	}

	private void processAddEntity(ByteBuffer data) {
		worldState.world.add(deserialize(data.get() == 1 ? true : false, getRemainingBytes(data)));
	}
	
	private static byte[] getRemainingBytes(ByteBuffer buffer) {
		byte[] data = new byte[buffer.remaining()];
		buffer.get(data);
		return data;
	}
	
	public void sendPlayerState(boolean left, boolean right, boolean up, boolean down, boolean primaryAction, boolean secondaryAction) {
		byte[] packet = new byte[3];
		Bytes.putShort(packet, 0, Protocol.TYPE_CLIENT_PLAYER_STATE);
		packet[2] = Protocol.PlayerState.getState(left, right, up, down, primaryAction, secondaryAction);
		client.sendUnreliable(packet);
	}

	private void processUpdateEntity(ByteBuffer data) {
		int entityID = data.getInt();
		for(Entity e : worldState.world) {
			if(e.getID() == entityID) {
				e.setPositionX(data.getFloat());
				e.setPositionY(data.getFloat());
				e.setVelocityX(data.getFloat());
				e.setVelocityY(data.getFloat());
				return;
			}
		}
	}

	private void processReceiveWorldData(ByteBuffer data) {
		WorldState state = this.worldState;
		if(state.data == null)
			throw new IllegalStateException("world head packet has not been received");
		int dataSize = data.remaining();
		data.get(state.data, state.data.length - state.worldBytesRemaining, dataSize);
		boolean receivedAll = (state.worldBytesRemaining -= dataSize) == 0;
		if(receivedAll) buildWorld();
	}
	
	private void buildWorld() {
		System.out.print("Building world... ");
		long start = System.nanoTime();
		worldState.world = deserialize(Protocol.COMPRESS_WORLD_DATA, worldState.data);
		System.out.println("took " + Utility.formatTime(Utility.nanosSince(start)) + ".");
		worldState.data = null; //release the raw data to the garbage collector
		client.sendReliable(Bytes.of(Protocol.TYPE_CLIENT_WORLD_BUILT));
	}

	private void onWorldJoin(World world, ClientPlayerEntity player) {
		worldState.world = world;
		this.player = player;
		cameraGrip = new TrackingCameraController(2.5f, 0.05f / player.getWidth(), 0.6f / player.getWidth());
		worldRenderer = new ClientWorldRenderer(StartClient.shaderProgram, cameraGrip.getCamera(), world);
		interactionControls = new InteractionController();
		StartClient.display.setCursor(StartClient.pickaxeCursor);
		lastWorldUpdate = System.nanoTime();
		lastCameraUpdate = System.nanoTime();
		GameLoop.setContext(this::updateGame);
	}
	
	private void waitForHandshake() {
		StartClient.display.poll(input);
		client.update(this::process);
	}

	private void updateGame() {
		StartClient.display.poll(input);

		boolean isLeft = StartClient.display.isControlActivated(Control.MOVE_LEFT);
		boolean isRight = StartClient.display.isControlActivated(Control.MOVE_RIGHT);
		boolean isUp = StartClient.display.isControlActivated(Control.MOVE_UP);
		boolean isDown = StartClient.display.isControlActivated(Control.MOVE_DOWN);
		boolean isPrimary = StartClient.display.isControlActivated(Control.USE_HELD_ITEM);
		boolean isSecondary = StartClient.display.isControlActivated(Control.THROW_BOMB);

		player.setLeft(isLeft);
		player.setRight(isRight);
		player.setUp(isUp);
		player.setDown(isDown);

		sendPlayerState(
			isLeft,
			isRight,
			isUp,
			isDown,
			isPrimary,
			isSecondary
		);

		interactionControls.update(StartClient.display, cameraGrip.getCamera(), this, worldState.world, player); //block breaking/"bomb throwing"
		client.update(this::process);
		if(!StartClient.display.minimized()) {
			updateVisuals();
		} else {
			lastWorldUpdate = System.nanoTime();
			lastCameraUpdate = System.nanoTime();
		}
		Utility.sleep(1); //reduce CPU usage
	}

	private void updateVisuals() {
		long start = System.nanoTime();
		if(start - lastWorldUpdate < StartClient.UPDATE_SKIP_THRESHOLD_NANOSECONDS) {
			Utility.updateWorld(worldState.world, lastWorldUpdate, Protocol.MAX_UPDATE_TIMESTEP, Protocol.TIME_SCALE_NANOSECONDS);
		}
		lastWorldUpdate = start;
		long camUpdateStart = System.nanoTime();
		cameraGrip.update(player, StartClient.audio, camUpdateStart - lastCameraUpdate); //update camera position and zoom
		lastCameraUpdate = camUpdateStart;
		RenderManager.run(StartClient.display, worldRenderer);
	}
	
	@Override
	public void sendBombThrow(float angle) {
		byte[] packet = new byte[2 + 4];
		Bytes.putShort(packet, 0, Protocol.TYPE_CLIENT_BOMB_THROW);
		Bytes.putFloat(packet, 2, angle);
		client.sendReliable(packet);
	}

	@Override
	public void sendBlockBreak(int x, int y) {
		byte[] packet = new byte[10];
		Bytes.putShort(packet, 0, Protocol.TYPE_CLIENT_BREAK_BLOCK);
		Bytes.putInteger(packet, 2, x);
		Bytes.putInteger(packet, 6, y);
		client.sendReliable(packet);
	}

	private static void onAccepted() {
		System.out.println("connected.");
	}

	private void onRejected() {
		System.out.println("rejected.");
		try {
			abort();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void onDisconnected() {
		try {
			StartClient.display.resetCursor();
			client.close();
			if(StartClient.display.wasClosed()) {
				StartClient.exit();
			} else {
				GameLoop.setContext(StartClient.mainMenu);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
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
			System.out.println("An exception occurred: " + (e.getMessage() == null ? e.getClass().getName() : e.getMessage()));
			abort();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void abort() throws IOException {
		client.close();
		GameLoop.setContext(StartClient.mainMenu);
	}
	
	InputContext input = new InputContext() {
		@Override
		public void windowFocus(boolean focused) {
			StartClient.audio.setVolume(focused ? 1.0f : 0.0f);
		}

		@Override
		public void windowClose() {
			leaveServer();
		}

		private final Map<Button, Runnable> controls = Map.ofEntries(
				Map.entry(Control.FULLSCREEN, StartClient.display::toggleFullscreen),
				Map.entry(Control.QUIT,  InWorldContext.this::leaveServer)
		);

		@Override
		public Map<Button, Runnable> buttonControls() {
			return controls;
		}

		@Override
		public void keyboardButton(int key, int scancode, int action, int mods) {
			if(cameraGrip != null)
				cameraGrip.keyboardButton(key, scancode, action, mods);
		}

		@Override
		public void mouseButton(int button, int action, int mods) {
			if(cameraGrip != null)
				cameraGrip.mouseButton(button, action, mods);
		}

		@Override
		public void mouseScroll(double xoffset, double yoffset) {
			if(cameraGrip != null)
				cameraGrip.mouseScroll(xoffset, yoffset);
		}

		@Override
		public void windowRefresh() {
			sendPlayerState(false, false, false, false, false, false);
			interactionControls.update(StartClient.display, 
					cameraGrip.getCamera(), InWorldContext.this, worldState.world, player); //block breaking/"bomb throwing"
			client.update(InWorldContext.this::process);
			updateVisuals();
		}
	};
}