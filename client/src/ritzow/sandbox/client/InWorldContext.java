package ritzow.sandbox.client;

import java.io.IOException;
import java.util.Map;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.Control.Button;
import ritzow.sandbox.client.input.InputContext;
import ritzow.sandbox.client.input.controller.InteractionController;
import ritzow.sandbox.client.input.controller.TrackingCameraController;
import ritzow.sandbox.client.network.Client;
import ritzow.sandbox.client.network.Client.ClientEvent;
import ritzow.sandbox.client.world.ClientWorldRenderer;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;

class InWorldContext implements GameContext {
	Client client;
	ClientWorldRenderer worldRenderer;
	TrackingCameraController cameraGrip;
	InteractionController interactionControls;
	ClientPlayerEntity player;
	World world;
	long lastWorldUpdate, lastCameraUpdate;

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
			client.sendPlayerState(false, false, false, false, false, false);
			interactionControls.update(StartClient.display, cameraGrip.getCamera(), client, world, player); //block breaking/"bomb throwing"
			client.update();
			updateVisuals();
		};
	};

	private void leaveServer() {
		client.startDisconnect();
		GameLoop.setContext(client::update);
	}

	@Override
	public void run() {
		try {
			System.out.print("Connecting to " + Utility.formatAddress(StartClient.serverAddress)
			+ " from " + Utility.formatAddress(StartClient.localAddress) + "... ");
			client = Client.create(StartClient.localAddress, StartClient.serverAddress);
			client.setEventListener(ClientEvent.CONNECT_ACCEPTED, this::onAccepted);
			client.setEventListener(ClientEvent.CONNECT_REJECTED, this::onRejected);
			client.setEventListener(ClientEvent.DISCONNECTED, this::onDisconnected);
			client.setEventListener(ClientEvent.TIMED_OUT, this::onTimeout);
			client.setEventListener(ClientEvent.WORLD_JOIN, this::onWorldJoin);
			client.setEventListener(ClientEvent.EXCEPTION_OCCURRED, this::onException);
			client.beginConnect();
			GameLoop.setContext(this::waitForHandshake);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void waitForHandshake() {
		StartClient.display.poll(input);
		client.update();
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

		client.sendPlayerState(
			isLeft,
			isRight,
			isUp,
			isDown,
			isPrimary,
			isSecondary
		);

		interactionControls.update(StartClient.display, cameraGrip.getCamera(), client, world, player); //block breaking/"bomb throwing"
		client.update();
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
			Utility.updateWorld(world, lastWorldUpdate, Protocol.MAX_UPDATE_TIMESTEP, Protocol.TIME_SCALE_NANOSECONDS);
		}
		lastWorldUpdate = start;
		long camUpdateStart = System.nanoTime();
		cameraGrip.update(player, StartClient.audio, camUpdateStart - lastCameraUpdate); //update camera position and zoom
		lastCameraUpdate = camUpdateStart;
		RenderManager.run(StartClient.display, worldRenderer);
	}

	private void onWorldJoin(World world, ClientPlayerEntity player) {
		this.world = world;
		this.player = player;
		cameraGrip = new TrackingCameraController(2.5f, 0.05f / player.getWidth(), 0.6f / player.getWidth());
		worldRenderer = new ClientWorldRenderer(StartClient.shaderProgram, cameraGrip.getCamera(), world);
		interactionControls = new InteractionController();
		StartClient.display.setCursor(StartClient.pickaxeCursor);
		lastWorldUpdate = System.nanoTime();
		lastCameraUpdate = System.nanoTime();
		GameLoop.setContext(this::updateGame);
	}

	private void onAccepted() {
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
}