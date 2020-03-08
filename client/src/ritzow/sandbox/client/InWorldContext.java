package ritzow.sandbox.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import ritzow.sandbox.client.audio.AudioSystem;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.client.graphics.Display;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.Control.Button;
import ritzow.sandbox.client.input.InputContext;
import ritzow.sandbox.client.input.controller.InteractionController;
import ritzow.sandbox.client.input.controller.TrackingCameraController;
import ritzow.sandbox.client.network.Client;
import ritzow.sandbox.client.network.GameTalker;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.client.util.SerializationProvider;
import ritzow.sandbox.client.world.ClientWorldRenderer;
import ritzow.sandbox.client.world.block.ClientBlockProperties;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Transportable;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.PlayerEntity;

import static ritzow.sandbox.client.util.ClientUtility.log;

class InWorldContext implements GameTalker {
	private final Client client;
	private final InputContext input;
	private final GameState state;
	InteractionController interactionControls;
	ClientWorldRenderer worldRenderer;
	TrackingCameraController cameraGrip;
	ClientPlayerEntity player;

	World world;
	ByteBuffer worldBuffer;
	int playerID;
	long lastGameUpdate;

	public InWorldContext(GameState state, Client client, int downloadSize, int playerID) {
		this.state = state;
		this.client = client;
		this.input = new InWorldInputContext();
		this.worldBuffer = ByteBuffer.allocate(downloadSize);
		this.playerID = playerID;
		log().info("Downloading " + Utility.formatSize(downloadSize) + " of world data");
	}

	private void update() {
		Display display = state.display;
		long frameStart = System.nanoTime();
		client.update(this::process);
		display.poll(input);
		if(isPaused()) {
			lastGameUpdate = System.nanoTime();
			sendPing();
		} else {
			updateRunning(display);
		}
		ClientUtility.limitFramerate(frameStart);
	}

	private void updateRunning(Display display) {
		if(display.isControlActivated(Control.RESET_ZOOM)) cameraGrip.resetZoom();
		boolean isLeft = display.isControlActivated(Control.MOVE_LEFT);
		boolean isRight = display.isControlActivated(Control.MOVE_RIGHT);
		boolean isUp = display.isControlActivated(Control.MOVE_UP);
		boolean isDown = display.isControlActivated(Control.MOVE_DOWN);
		boolean isPrimary = display.isControlActivated(Control.USE_HELD_ITEM);
		boolean isSecondary = display.isControlActivated(Control.THROW_BOMB);
		player.setLeft(isLeft);
		player.setRight(isRight);
		player.setUp(isUp);
		player.setDown(isDown);
		sendPlayerState(player, isPrimary, isSecondary);
		updateRender(display);
	}

	private void updateRender(Display display) {
		long start = System.nanoTime();
		long deltaTime = start - lastGameUpdate;
		lastGameUpdate = start;
		world.update(deltaTime);
		cameraGrip.update(display, player, AudioSystem.getDefault(), deltaTime);
		int width = display.width(), height = display.height();
		RenderManager.preRender(width, height);
		worldRenderer.render(RenderManager.DISPLAY_BUFFER, width, height);
		interactionControls.update(display, cameraGrip.getCamera(), this, world, player);
		interactionControls.render(display, state.shader, cameraGrip.getCamera(), world, player);
		display.refresh();
	}

	private void process(short messageType, ByteBuffer data) {
		switch(messageType) {
			case Protocol.TYPE_CONSOLE_MESSAGE -> processServerConsoleMessage(data);
			case Protocol.TYPE_SERVER_WORLD_DATA -> processReceiveWorldData(data);
			case Protocol.TYPE_SERVER_ENTITY_UPDATE -> processUpdateEntity(data);
			case Protocol.TYPE_SERVER_CREATE_ENTITY -> processAddEntity(data);
			case Protocol.TYPE_SERVER_DELETE_ENTITY -> processRemoveEntity(data);
			case Protocol.TYPE_SERVER_REMOVE_BLOCK -> processServerRemoveBlock(data);
			case Protocol.TYPE_SERVER_PLACE_BLOCK -> processServerPlaceBlock(data);
			case Protocol.TYPE_CLIENT_PLAYER_STATE -> processPlayerState(data);
			case Protocol.TYPE_SERVER_CLIENT_DISCONNECT -> processServerDisconnect(data);
			case Protocol.TYPE_SERVER_CLIENT_BREAK_BLOCK_COOLDOWN -> processBlockBreakCooldown(data);
			default -> throw new IllegalArgumentException("Client received message of unknown protocol " + messageType);
		}
	}

	private void processBlockBreakCooldown(ByteBuffer data) {
		//interactionControls.setLastBreak(data.getLong());
	}

	private static void processServerConsoleMessage(ByteBuffer data) {
		log().info(Bytes.getString(data, data.remaining(), Protocol.CHARSET));
	}

	private void leaveServer() {
		startDisconnect();
		GameLoop.setContext(() -> client.update(this::process));
	}

	/**
	 * Notifies the server, disconnects, and stops the client. May block
	 * while disconnect is occurring.
	 */
	public void startDisconnect() {
		log().info("Starting disconnect");
		client.sendReliable(Bytes.of(Protocol.TYPE_CLIENT_DISCONNECT), this::onDisconnected);
	}

	private void processServerDisconnect(ByteBuffer data) {
		log().info("Disconnected from server: "
				+ Bytes.getString(data, data.getInt(), Protocol.CHARSET));
		onDisconnected();
	}

	private void onDisconnected() {
		try {
			client.close();
			log().info("Disconnected from server");
		} catch (IOException e) {
			log().log(Level.SEVERE, "Exception on Client::close()", e);
		} finally {
			state.display.resetCursor();
			if(state.display.wasClosed()) {
				GameLoop.stop();
			} else {
				GameLoop.setContext(state.menuContext::update);
			}
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
		ClientBlockProperties newBlock = deserialize(data);
		world.getForeground().set(x, y, newBlock);
		newBlock.onPlace(world, world.getForeground(), cameraGrip.getCamera(), x, y);
	}

	@SuppressWarnings("unchecked")
	private <E extends Entity> E getEntity(int id) {
		return Objects.requireNonNull((E)world.getEntityFromID(id), "No entity with ID " + id + " exists");
	}

	private void processRemoveEntity(ByteBuffer data) {
		world.remove(world.getEntityFromID(data.getInt()));
	}

	private static <T extends Transportable> T deserialize(ByteBuffer data) {
		return SerializationProvider.getProvider().deserialize(data);
	}

	private void processAddEntity(ByteBuffer data) {
		Entity entity = deserialize(data.get() == 1 ? Bytes.decompress(data) : data);
		if(entity instanceof PlayerEntity) log().info("Another player connected to the server");
		world.add(entity);
	}

	private void processUpdateEntity(ByteBuffer data) {
		for(int count = data.getInt(); count > 0; --count) {
			Entity e = getEntity(data.getInt());
			e.setPositionX(data.getFloat());
			e.setPositionY(data.getFloat());
			e.setVelocityX(data.getFloat());
			e.setVelocityY(data.getFloat());
		}
	}

	private void processReceiveWorldData(ByteBuffer data) {
		if(!worldBuffer.put(data).hasRemaining()) {
			log().info("Received all world data");
			long start = System.nanoTime();
			world = SerializationProvider.getProvider().deserialize(Protocol.COMPRESS_WORLD_DATA ?
						Bytes.decompress(worldBuffer.flip()) : worldBuffer.flip());
			worldBuffer = null; //release the raw data to the garbage collector
			log().info("World built, took " + Utility.formatTime(Utility.nanosSince(start)));
			player = getEntity(playerID);
			cameraGrip = new TrackingCameraController(2.5f, player.getWidth() / 20f, player.getWidth() / 2f);
			worldRenderer = new ClientWorldRenderer(state.shader, cameraGrip.getCamera(), world);
			interactionControls = new InteractionController();
			state.display.setCursor(state.cursorPick);
			client.sendReliable(Bytes.of(Protocol.TYPE_CLIENT_WORLD_BUILT));
			lastGameUpdate = System.nanoTime();
			GameLoop.setContext(this::update);
		}
	}

	public void listenForServer() {
		state.display.poll(input);
		client.update(this::process);
	}

	private boolean isPaused() {
		return state.display.minimized();
	}

	public void sendPlayerState(PlayerEntity player, boolean primary, boolean secondary) {
		byte[] packet = new byte[4];
		Bytes.putShort(packet, 0, Protocol.TYPE_CLIENT_PLAYER_STATE);
		Bytes.putShort(packet, 2, Protocol.PlayerState.getState(player, primary, secondary));
		client.sendUnreliable(packet);
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

	private void sendPing() {
		client.sendReliable(Bytes.of(Protocol.TYPE_PING));
	}

	private void selectSlot(int slot) {
		long cursor = switch(slot) {
			case 1, 2 -> state.cursorMallet;
			default -> state.cursorPick;
		};
		player.setSlot(slot);
		state.display.setCursor(cursor);
	}

	private final class InWorldInputContext implements InputContext {
		@Override
		public void windowFocus(boolean focused) {
			AudioSystem.getDefault().setVolume(focused ? 1.0f : 0.0f);
		}

		@Override
		public void windowRefresh() {
			sendPing();
			client.update(InWorldContext.this::process);
			updateRender(state.display);
		}

		@Override
		public void windowClose() {
			leaveServer();
		}

		private final Map<Button, Runnable> controls = Map.ofEntries(
			Map.entry(Control.FULLSCREEN, state.display::toggleFullscreen),
			Map.entry(Control.QUIT,  InWorldContext.this::leaveServer),
			Map.entry(Control.SLOT_SELECT_1, () -> selectSlot(0)),
			Map.entry(Control.SLOT_SELECT_2, () -> selectSlot(1)),
			Map.entry(Control.SLOT_SELECT_3, () -> selectSlot(2))
		);

		@Override
		public Map<Button, Runnable> buttonControls() {
			return controls;
		}

		double slotOffset = 0;

		@Override
		public void mouseScroll(double xoffset, double yoffset) {
			if(state.display.isControlActivated(Control.SCROLL_MODIFIER)) {
				if(slotOffset == 0) slotOffset += yoffset;

				if(slotOffset > 0) {
					slotOffset = yoffset >= 0 ? slotOffset + yoffset : 0;
					if(slotOffset > StandardClientOptions.SELECT_SENSITIVITY) {
						selectSlot(Math.max(player.selected() - 1, 0));
						slotOffset = 0;
					}
				} else if(slotOffset < 0) {
					slotOffset = yoffset <= 0 ? slotOffset + yoffset : 0;
					if(slotOffset < -StandardClientOptions.SELECT_SENSITIVITY) {
						selectSlot(Math.min(player.selected() + 1, player.inventory().getSize() - 1));
						slotOffset = 0;
					}
				}
			} else if(cameraGrip != null) cameraGrip.mouseScroll(xoffset, yoffset);
		}
	}
}