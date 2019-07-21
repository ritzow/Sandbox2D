package ritzow.sandbox.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import ritzow.sandbox.client.graphics.Display;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.Control.Button;
import ritzow.sandbox.client.input.InputContext;
import ritzow.sandbox.client.input.controller.InteractionController;
import ritzow.sandbox.client.input.controller.TrackingCameraController;
import ritzow.sandbox.client.network.Client;
import ritzow.sandbox.client.network.Client.Status;
import ritzow.sandbox.client.network.GameTalker;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.client.util.SerializationProvider;
import ritzow.sandbox.client.world.ClientWorldRenderer;
import ritzow.sandbox.client.world.block.ClientBlockProperties;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.PlayerEntity;

public class InWorldContext implements GameTalker {
	Client client;
	World world;
	byte[] worldData;
	int worldBytesRemaining;
	InteractionController interactionControls;
	ClientWorldRenderer worldRenderer;
	TrackingCameraController cameraGrip;
	ClientPlayerEntity player;
	long lastWorldUpdate, lastCameraUpdate;
	
	public InWorldContext(Client client, int downloadSize) {
		this.client = client;
		this.worldData = new byte[downloadSize];
		this.worldBytesRemaining = downloadSize;
	}
	
	private void leaveServer() {
		startDisconnect();
		GameLoop.setContext(() -> client.update(this::process));
	}

	private void process(short messageType, ByteBuffer data) {
		switch(messageType) {
			case Protocol.TYPE_CONSOLE_MESSAGE -> processServerConsoleMessage(data);
			case Protocol.TYPE_SERVER_WORLD_DATA -> processReceiveWorldData(data);
			case Protocol.TYPE_SERVER_ENTITY_UPDATE -> processUpdateEntity(data);
			case Protocol.TYPE_SERVER_ADD_ENTITY -> processAddEntity(data);
			case Protocol.TYPE_SERVER_REMOVE_ENTITY -> processRemoveEntity(data);
			case Protocol.TYPE_SERVER_PLAYER_ID -> processReceivePlayerEntityID(data);
			case Protocol.TYPE_SERVER_REMOVE_BLOCK -> processServerRemoveBlock(data);
			case Protocol.TYPE_SERVER_PLACE_BLOCK -> processServerPlaceBlock(data);
			case Protocol.TYPE_CLIENT_PLAYER_STATE -> processPlayerState(data);
			case Protocol.TYPE_SERVER_CLIENT_DISCONNECT -> processServerDisconnect(data);
			case Protocol.TYPE_SERVER_PING -> {}
			default -> throw new IllegalArgumentException("Client received message of unknown protocol " + messageType);
		}
	}

	private static void log(String message) {
		System.out.println(Utility.formatCurrentTime() + " " + message);
	}
	
	private static void processServerConsoleMessage(ByteBuffer data) {
		log(new String(getRemainingBytes(data), Protocol.CHARSET));
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
			onDisconnected();
		});
	}
	
	private void processServerDisconnect(ByteBuffer data) {
		client.setStatus(Status.DISCONNECTED);
		int length = data.getInt();
		byte[] array = new byte[length];
		data.get(array);
		log("Disconnected from server: " + new String(array, Protocol.CHARSET));
		onDisconnected();
	}
	
	private void onDisconnected() {
		try {
			StartClient.display.resetCursor();
			client.close();
			if(StartClient.display.wasClosed()) {
				StartClient.exit();
			} else {
				System.out.println("Disconnected from server.");
				GameLoop.setContext(StartClient.mainMenu::update);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void processPlayerState(ByteBuffer data) {
		int id = data.getInt();
		if(id != player.getID()) {
			Protocol.PlayerState.updatePlayer(getEntity(id), data.getShort());
		}
	}

	private void processServerRemoveBlock(ByteBuffer data) {
		int x = data.getInt(), y = data.getInt();
		ClientBlockProperties prev = (ClientBlockProperties)world.getForeground().set(x, y, null);
		if(prev != null) prev.onBreak(world, world.getForeground(), cameraGrip.getCamera(), x, y);
	}
	
	private void processServerPlaceBlock(ByteBuffer data) {
		int x = data.getInt(), y = data.getInt();
		byte[] block = new byte[data.remaining()];
		data.get(block);
		ClientBlockProperties newBlock = deserialize(true, block);
		world.getForeground().set(x, y, newBlock);
		newBlock.onPlace(world, world.getForeground(), cameraGrip.getCamera(), x, y);
	}

	private void processReceivePlayerEntityID(ByteBuffer data) {
		int id = data.getInt();
		this.player = getEntity(id);
		onWorldJoin();
	}
	
	@SuppressWarnings("unchecked")
	private <E extends Entity> E getEntity(int id) {
		return Objects.requireNonNull((E)world.getEntityFromID(id), "No entity with ID " + id + " exists");
	}

	private void processRemoveEntity(ByteBuffer data) {
		int id = data.getInt();
		world.remove(world.getEntityFromID(id));
	}

	private static <T> T deserialize(boolean compress, byte[] data) {
		return SerializationProvider.getProvider().deserialize(compress ? Bytes.decompress(data) : data);
	}

	private void processAddEntity(ByteBuffer data) {
		world.add(deserialize(data.get() == 1 ? true : false, getRemainingBytes(data)));
	}
	
	private static byte[] getRemainingBytes(ByteBuffer buffer) {
		byte[] data = new byte[buffer.remaining()];
		buffer.get(data);
		return data;
	}

	private void processUpdateEntity(ByteBuffer data) {
		int count = data.getInt();
		while(count > 0) {
			int entityID = data.getInt();
			Entity e = getEntity(entityID);
			if(e != null) {
				e.setPositionX(data.getFloat());
				e.setPositionY(data.getFloat());
				e.setVelocityX(data.getFloat());
				e.setVelocityY(data.getFloat());
			}
			--count;
		}
	}

	private void processReceiveWorldData(ByteBuffer data) {
		if(this.worldData == null)
			throw new IllegalStateException("world head packet has not been received");
		int dataSize = data.remaining();
		data.get(this.worldData, this.worldData.length - worldBytesRemaining, dataSize);
		boolean receivedAll = (worldBytesRemaining -= dataSize) == 0;
		if(receivedAll) buildWorld();
	}
	
	private void buildWorld() {
		System.out.print("received. Building world... ");
		long start = System.nanoTime();
		world = deserialize(Protocol.COMPRESS_WORLD_DATA, worldData);
		worldData = null; //release the raw data to the garbage collector
		client.sendReliable(Bytes.of(Protocol.TYPE_CLIENT_WORLD_BUILT));
		System.out.println("took " + Utility.formatTime(Utility.nanosSince(start)) + ".");
	}

	private void onWorldJoin() {
		cameraGrip = new TrackingCameraController(2.5f, player.getWidth() / 20f, player.getWidth() / 2f);
		worldRenderer = new ClientWorldRenderer(StartClient.shaderProgram, cameraGrip.getCamera(), world);
		interactionControls = new InteractionController();
		StartClient.display.setCursor(StartClient.pickaxeCursor);
		lastWorldUpdate = System.nanoTime();
		lastCameraUpdate = System.nanoTime();
		GameLoop.setContext(this::updateClient);
	}
	
	public void listenForServer() {
		StartClient.display.poll(input);
		client.update(this::process);
	}

	private void updateClient() {
		long frameStart = System.nanoTime();
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
		
		sendPlayerState(player, isPrimary, isSecondary);
		
		if(StartClient.display.isControlActivated(Control.RESET_ZOOM)) {
			cameraGrip.resetZoom();
		}

		interactionControls.update(
			StartClient.display, 
			cameraGrip.getCamera(), 
			this, world, player
		); //block breaking/"bomb throwing"
		client.update(this::process);
		if(!StartClient.display.minimized()) {
			updateVisuals();
		} else {
			lastWorldUpdate = System.nanoTime();
			lastCameraUpdate = System.nanoTime();
		}
		ClientUtility.limitFramerate(frameStart);
	}
	
	public void sendPlayerState(PlayerEntity player, boolean primary, boolean secondary) {
		byte[] packet = new byte[4];
		Bytes.putShort(packet, 0, Protocol.TYPE_CLIENT_PLAYER_STATE);
		Bytes.putShort(packet, 2, Protocol.PlayerState.getState(player, primary, secondary));
		client.sendUnreliable(packet);
	}

	private void updateVisuals() {
		long start = System.nanoTime();
		if(start - lastWorldUpdate < StartClient.UPDATE_SKIP_THRESHOLD_NANOSECONDS) {
			Utility.updateWorld(world, lastWorldUpdate, Protocol.MAX_UPDATE_TIMESTEP);
		}
		Display display = StartClient.display;
		lastWorldUpdate = start;
		long camUpdateStart = System.nanoTime();
		cameraGrip.update(display, player, 
				StartClient.audio, camUpdateStart - lastCameraUpdate); //update camera position and zoom
		lastCameraUpdate = camUpdateStart;
		int width = display.width(), height = display.height();
		RenderManager.preRender(width, height);
		worldRenderer.render(RenderManager.DISPLAY_BUFFER, width, height);
		interactionControls.render(display, StartClient.shaderProgram, 
				cameraGrip.getCamera(), world.getForeground(), player);
		display.refresh();
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
	
	@Override
	public void sendBlockPlace(int x, int y) {
		byte[] packet = new byte[10];
		Bytes.putShort(packet, 0, Protocol.TYPE_CLIENT_PLACE_BLOCK);
		Bytes.putInteger(packet, 2, x);
		Bytes.putInteger(packet, 6, y);
		client.sendReliable(packet);
	}
	
	private void selectSlot(int slot) {
		long cursor = switch(slot) {
			case 1, 2 -> StartClient.malletCursor;
			default -> StartClient.pickaxeCursor;
		};
		player.setSlot(slot);
		StartClient.display.setCursor(cursor);
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
				Map.entry(Control.QUIT,  InWorldContext.this::leaveServer),
				Map.entry(Control.SLOT_SELECT_1, () -> selectSlot(0)),
				Map.entry(Control.SLOT_SELECT_2, () -> selectSlot(1)),
				Map.entry(Control.SLOT_SELECT_3, () -> selectSlot(2))
		);

		@Override
		public Map<Button, Runnable> buttonControls() {
			return controls;
		}
		
		int slotOffset = 0;

		@Override
		public void mouseScroll(double xoffset, double yoffset) {
			if(StartClient.display.isControlActivated(Control.SCROLL_MODIFIER)) {
				slotOffset -= yoffset;
				if(Math.abs(slotOffset) >= 1) {
					int slot = Math.max(0, Math.min(player.selected() + slotOffset, player.inventory().getSize()-1));
					selectSlot(slot);	
					slotOffset = 0;
				}
			} else if(cameraGrip != null) cameraGrip.mouseScroll(xoffset, yoffset);
		}

		@Override
		public void windowRefresh() {
			sendPlayerState(player, false, false);
			interactionControls.update(StartClient.display,
					cameraGrip.getCamera(), InWorldContext.this, world, player); //block breaking/"bomb throwing"
			client.update(InWorldContext.this::process);
			updateVisuals();
		}
	};
}