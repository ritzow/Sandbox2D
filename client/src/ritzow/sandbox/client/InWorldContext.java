package ritzow.sandbox.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.DoubleConsumer;
import java.util.logging.Level;
import ritzow.sandbox.client.audio.AudioSystem;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.input.Button;
import ritzow.sandbox.client.input.ControlsContext;
import ritzow.sandbox.client.input.controller.InteractionController;
import ritzow.sandbox.client.input.controller.TrackingCameraController;
import ritzow.sandbox.client.network.Client;
import ritzow.sandbox.client.network.GameTalker;
import ritzow.sandbox.client.network.ServerBadDataException;
import ritzow.sandbox.client.ui.GuiElement;
import ritzow.sandbox.client.ui.Position;
import ritzow.sandbox.client.ui.element.*;
import ritzow.sandbox.client.ui.element.BorderAnchor.Anchor;
import ritzow.sandbox.client.ui.element.BorderAnchor.Side;
import ritzow.sandbox.client.ui.element.VBoxDynamic.Alignment;
import ritzow.sandbox.client.util.SerializationProvider;
import ritzow.sandbox.client.world.ClientWorldRendererLightmap;
import ritzow.sandbox.client.world.block.ClientBlockProperties;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Transportable;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.PlayerEntity;

import static ritzow.sandbox.client.input.Control.*;
import static ritzow.sandbox.client.util.ClientUtility.log;
import static ritzow.sandbox.network.Protocol.*;

class InWorldContext implements GameTalker {
	private static final long PLAYER_STATE_SEND_INTERVAL = Utility.frameRateToFrameTimeNanos(60);

	private final Client client;
	private final int playerID;
	private InteractionController interactionControls;
	private ClientWorldRendererLightmap worldRenderer;
	private TrackingCameraController cameraGrip;
	private ClientPlayerEntity player;
	private GuiElement overlayGUI;
	private Holder<Icon> blockGUI;
	private EditableText framerateDisplay;
	private World world;

	private ByteBuffer worldDownloadBuffer;
	private final DoubleConsumer downloadProgressAction;
	private CompletableFuture<World> worldBuildTask;

	private final ControlsContext controlsContext = new ControlsContext(
		FULLSCREEN,
		QUIT,
		ZOOM_INCREASE,
		ZOOM_DECREASE,
		ZOOM_RESET,
		SCROLL_MODIFIER,
		USE_HELD_ITEM,
		THROW_BOMB,
		MOVE_UP,
		MOVE_DOWN,
		MOVE_LEFT,
		MOVE_RIGHT,
		SLOT_SELECT_1,
		SLOT_SELECT_2,
		SLOT_SELECT_3) {

		@Override
		public void framebufferSize(int width, int height) {
			worldRenderer.updateFramebuffers(width, height);
		}

		@Override
		public void windowFocus(boolean focused) {
			AudioSystem.getDefault().setVolume(focused ? 1.0f : 0.0f);
		}

		@Override
		public void windowRefresh() {
			sendPing();
			client.update(InWorldContext.this::process);
			updateRender(GameState.display(), GameLoop.updateDeltaTime());
		}

		@Override
		public void windowClose() {
			leaveServer();
		}

		double slotOffset = 0;

		@Override
		public void mouseScroll(double xoffset, double yoffset) {
			if(controlsContext.isPressed(SCROLL_MODIFIER)) {
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
			} else if(cameraGrip != null) {
				cameraGrip.zoom((float)yoffset);
			}
		}
	};

	private final Map<Button, Runnable> controls = Map.ofEntries(
		Map.entry(FULLSCREEN, GameState.display()::toggleFullscreen),
		Map.entry(QUIT,  InWorldContext.this::leaveServer),
		Map.entry(SLOT_SELECT_1, () -> selectSlot(0)),
		Map.entry(SLOT_SELECT_2, () -> selectSlot(1)),
		Map.entry(SLOT_SELECT_3, () -> selectSlot(2))
	);

	public InWorldContext(Client client, int downloadSize, int playerID, DoubleConsumer downloadProgress) {
		log().info("Downloading " + Utility.formatSize(downloadSize) + " of world data");
		this.client = client;
		this.worldDownloadBuffer = ByteBuffer.allocate(downloadSize);
		this.downloadProgressAction = downloadProgress;
		downloadProgressAction.accept(0);
		this.playerID = playerID;
	}

	public void updateJoining() {
		if(worldBuildTask == null) {
			client.update(data -> {
				short type = data.getShort();
				if(type == TYPE_SERVER_WORLD_DATA) {
					downloadProgressAction.accept(worldDownloadBuffer.put(data).position()/(double)worldDownloadBuffer.limit());
					if(!worldDownloadBuffer.hasRemaining()) {
						log().info("Received all world data");
						worldBuildTask = CompletableFuture.supplyAsync(this::buildWorld);
					}
				} else {
					throw new ServerBadDataException("Received incorrect message type " + type + " during world download");
				}
				return true;
			});
		} else if(worldBuildTask.isDone()) {
			setupAfterReceiveWorld();
		} //TODO else maybe send a ping back to server or something?
	}

	private void setupAfterReceiveWorld() {
		world = worldBuildTask.join();
		worldDownloadBuffer = null; //release the downloaded data to the garbage collector
		worldBuildTask = null;
		player = getEntity(playerID);
		cameraGrip = new TrackingCameraController(2.5f, player.getWidth() / 20f, player.getWidth() / 2f);
		worldRenderer = new ClientWorldRendererLightmap(GameState.display(), cameraGrip.getCamera(), world);
		interactionControls = new InteractionController();

		GuiElement fpsBg = new Scaler(new Icon(GameModels.MODEL_SKY), 0.25f);

		//TODO create visual inventory that puts items in a small rectangular region that acts as backpack (drag around or outside to drop item)
		overlayGUI = new BorderAnchor(
			new Anchor(new ritzow.sandbox.client.ui.element.Button(new Text("Quit", RenderManager.FONT, 5, 0), this::leaveServer), Side.TOP_RIGHT, 0.1f, 0.1f),
			new Anchor(
				new Scaler(
					blockGUI = new Holder<>(
						new Icon(GameModels.MODEL_RED_SQUARE)
					), 0.5f
				), Side.BOTTOM_LEFT, 0.1f, 0.1f
			),
			new Anchor(new VBoxDynamic(0.05f, Alignment.LEFT,
//				new AbsoluteGuiPositioner(
//					//Map.entry(new HBox(0, fpsBg, fpsBg, fpsBg, fpsBg, fpsBg, fpsBg, fpsBg, fpsBg), Position.of(0, 0)),
//					Map.entry(framerateDisplay = new EditableText(RenderManager.FONT, 4, 0), Position.of(5, 0))
//				),
				framerateDisplay = new EditableText(RenderManager.FONT, 4, 0),
				new Text(NetworkUtility.formatAddress(client.getServerAddress()), RenderManager.FONT, 6, 0)
			), Side.TOP_LEFT, 0.05f, 0.05f)
		);

		GameState.display().setCursor(GameState.cursorPick());
		client.setOnTimeout(() -> {
			log().info("Timed out");
			onDisconnected();
		});
		client.sendReliable(Bytes.of(TYPE_CLIENT_WORLD_BUILT));
		log().info("World built, joined world");
		GameLoop.setContext(this::updateInWorld);
	}

	private void updateInWorld(long delta) {
		client.update(this::process);
		Display display = GameState.display();
		display.handleEvents(controlsContext);
		for(var entry : controls.entrySet()) {
			if(controlsContext.isNewlyPressed(entry.getKey())) {
				entry.getValue().run();
			}
		}
		updatePlayerState();
		if(!display.minimized()) { //TODO need to be more checks, this will run no matter what
			if(controlsContext.isNewlyPressed(ZOOM_RESET)) cameraGrip.resetZoom();
			updateRender(display, delta);
		}
	}

	private long lastPlayerStateSend;
	short lastPlayerState = 0;

	private void updatePlayerState() {
		boolean isPrimary = controlsContext.isPressed(USE_HELD_ITEM);
		boolean isSecondary = controlsContext.isPressed(THROW_BOMB);
		player.setLeft(controlsContext.isPressed(MOVE_LEFT));
		player.setRight(controlsContext.isPressed(MOVE_RIGHT));
		player.setUp(controlsContext.isPressed(MOVE_UP));
		player.setDown(controlsContext.isPressed(MOVE_DOWN));
		short playerState = PlayerState.getState(player, isPrimary, isSecondary);

		if(playerState != lastPlayerState || Utility.nanosSince(lastPlayerStateSend) > PLAYER_STATE_SEND_INTERVAL) {
			//The player may do something client side, but not send the player state
			//Solution: only send player state MORE FREQUENTLY (every frame) if it has changed from last state, and ALWAYS send it in that case
			byte[] packet = new byte[4];
			Bytes.putShort(packet, 0, TYPE_CLIENT_PLAYER_STATE);
			Bytes.putShort(packet, 2, playerState);
			client.sendUnreliable(packet);
			lastPlayerStateSend = System.nanoTime();
		}
	}

	private void updateRender(Display display, long deltaTime) {
		if(!StandardClientOptions.DISABLE_CLIENT_UPDATE) {
			world.update(deltaTime); //TODO should still have a max update step
		}
		cameraGrip.update(controlsContext, player, AudioSystem.getDefault(), deltaTime);
		int width = display.width(), height = display.height();
		worldRenderer.render(RenderManager.DISPLAY_BUFFER, width, height, computeDaylight());
		interactionControls.updateRender(display, RenderManager.DISPLAY_BUFFER, controlsContext, GameState.modelRenderer(), cameraGrip.getCamera(), this, world, player);
		GraphicsUtility.checkErrors();
		framerateDisplay.setContent(Utility.frameTimeToString(GameLoop.getLastUpdateTime())
			+ " Used: " + Utility.formatSize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
			+ " Total: " + Utility.formatSize(Runtime.getRuntime().totalMemory())
			+ " Max: " + Utility.formatSize(Runtime.getRuntime().maxMemory()));
		GameState.guiRenderer().render(overlayGUI, display, controlsContext, deltaTime, StandardClientOptions.GUI_SCALE);
		GraphicsUtility.checkErrors();
		GameState.modelRenderer().flush();
		RenderManager.postRender(GameState.display());
	}

	private static float computeDaylight() {
		return 1.0f; //Utility.oscillate(System.currentTimeMillis(), Utility.degreesPerSecToRadiansPerMillis(5), 0, 0.0f, 1.0f);
	}

	private boolean process(ByteBuffer data) {
		short messageType = data.getShort();
		switch(messageType) {
			case TYPE_CONSOLE_MESSAGE -> processServerConsoleMessage(data);
			case TYPE_SERVER_ENTITY_UPDATE -> processUpdateEntity(data);
			case TYPE_SERVER_CREATE_ENTITY -> processAddEntity(data);
			case TYPE_SERVER_DELETE_ENTITY -> processRemoveEntity(data);
			case TYPE_SERVER_REMOVE_BLOCK -> processServerRemoveBlock(data);
			case TYPE_SERVER_PLACE_BLOCK -> processServerPlaceBlock(data);
			case TYPE_CLIENT_PLAYER_STATE -> processPlayerState(data);
			case TYPE_SERVER_CLIENT_DISCONNECT -> processServerDisconnect(data);
			case TYPE_SERVER_CLIENT_SLOT_USE_COOLDOWN -> processBlockBreakCooldown(data);
			default -> log().severe("Client received message of unknown protocol " + messageType);
		}
		return true;
	}

	private void processBlockBreakCooldown(ByteBuffer data) {
		interactionControls.setNextUseTime(Instant.ofEpochMilli(data.getLong()));
	}

	private static void processServerConsoleMessage(ByteBuffer data) {
		log().info(Bytes.getString(data, data.remaining(), CHARSET));
	}

	/**
	 * Notifies the server, disconnects, and stops the client. May block
	 * while disconnect is occurring. */
	private void leaveServer() {
		log().info("Starting disconnect");
		client.sendReliable(Bytes.of(TYPE_CLIENT_DISCONNECT), this::onDisconnected);
	}

	private void processServerDisconnect(ByteBuffer data) {
		log().info("Disconnected from server: " + Bytes.getString(data, data.getInt(), CHARSET));
		onDisconnected();
	}

	private void onDisconnected() {
		try {
			client.close();
			log().info("Disconnected from server");
		} catch (IOException e) {
			log().log(Level.SEVERE, "Exception on Client::close()", e);
		} finally {
			GameState.display().resetCursor();
			if(GameState.display().wasClosed()) {
				GameLoop.setContext(StartClient::shutdown);
			} else {
				GameLoop.setContext(GameState.menuContext()::onLeaveWorld);
			}
		}
	}

	private static <T extends Transportable> T deserialize(ByteBuffer data) {
		return SerializationProvider.getProvider().deserialize(data);
	}

	private void processPlayerState(ByteBuffer data) {
		int id = data.getInt();
		if(id != player.getID()) {
			PlayerState.updatePlayer(getEntity(id), data.getShort());
		}
	}

	private void processServerRemoveBlock(ByteBuffer data) {
		int x = data.getInt(), y = data.getInt();
		int layer = world.getBlocks().getTopBlockLayer(x, y);
		if(layer >= 0) {
			Block block = world.getBlocks().get(layer, x, y);
			ClientBlockProperties prev = (ClientBlockProperties)world.getBlocks().set(layer, x, y, null);
			if(prev != null) prev.onBreak(world, world.getBlocks(), cameraGrip.getCamera(), x, y);
			worldRenderer.updateLighting(x, y);
		}
	}

	private void processServerPlaceBlock(ByteBuffer data) {
		int x = data.getInt(), y = data.getInt();
		ClientBlockProperties newBlock = deserialize(data);
		world.getBlocks().set(Utility.getBlockPlaceLayer(world.getBlocks(), x, y), x, y, newBlock);
		newBlock.onPlace(world, world.getBlocks(), cameraGrip.getCamera(), x, y);
		worldRenderer.updateLighting(x, y);
	}

	@SuppressWarnings("unchecked")
	private <E extends Entity> E getEntity(int id) {
		return (E)world.getEntityFromID(id);
	}

	private void processRemoveEntity(ByteBuffer data) {
		Entity entity = world.remove(data.getInt());
		if(entity instanceof Lit e) {
			worldRenderer.removeLight(e);
		}
	}

	private void processAddEntity(ByteBuffer data) {
		Entity entity = deserialize(data.get() == 1 ? Bytes.decompress(data) : data);
		if(entity instanceof PlayerEntity) log().info("Another player connected to the server");
		world.add(entity);
		if(entity instanceof Lit e) {
			worldRenderer.addLight(e);
		}
	}

	private void processUpdateEntity(ByteBuffer data) {
		for(int count = data.getInt(); count > 0; --count) {
			Entity e = world.getEntityFromIdOrNull(data.getInt());
			if(e != null) {
				e.setPositionX(data.getFloat());
				e.setPositionY(data.getFloat());
				e.setVelocityX(data.getFloat());
				e.setVelocityY(data.getFloat());
			}
		}
	}

	private World buildWorld() {
		return SerializationProvider.getProvider().deserialize(COMPRESS_WORLD_DATA ?
			Bytes.decompress(worldDownloadBuffer.flip()) : worldDownloadBuffer.flip());
	}

	@Override
	public void sendBombThrow(float angle) {
		byte[] packet = new byte[2 + 4];
		Bytes.putShort(packet, 0, TYPE_CLIENT_BOMB_THROW);
		Bytes.putFloat(packet, 2, angle);
		client.sendReliable(packet);
	}

	@Override
	public void sendBlockBreak(int x, int y) {
		byte[] packet = new byte[10];
		Bytes.putShort(packet, 0, TYPE_CLIENT_BREAK_BLOCK);
		Bytes.putInteger(packet, 2, x);
		Bytes.putInteger(packet, 6, y);
		client.sendReliable(packet);
	}

	@Override
	public void sendBlockPlace(int x, int y) {
		byte[] packet = new byte[10];
		Bytes.putShort(packet, 0, TYPE_CLIENT_PLACE_BLOCK);
		Bytes.putInteger(packet, 2, x);
		Bytes.putInteger(packet, 6, y);
		client.sendReliable(packet);
	}

	private void sendPing() {
		client.sendReliable(Bytes.of(TYPE_PING));
	}

	private void selectSlot(int slot) {
		player.setSlot(slot);
		GameState.display().setCursor(switch(slot) {
			case SLOT_PLACE_GRASS -> {
				blockGUI.set(new Icon(GameModels.MODEL_GRASS_BLOCK));
				yield GameState.cursorMallet();
			}
			case SLOT_PLACE_DIRT -> {
				blockGUI.set(new Icon(GameModels.MODEL_DIRT_BLOCK));
				yield GameState.cursorMallet();
			}

			case SLOT_PLACE_GLASS -> {
				blockGUI.set(new Icon(GameModels.MODEL_GLASS_BLOCK));
				yield GameState.cursorMallet();
			}

			default -> {
				blockGUI.set(new Icon(GameModels.MODEL_RED_SQUARE));
				yield GameState.cursorPick();
			}
		});
	}
}