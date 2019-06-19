package ritzow.sandbox.client;

import java.io.IOException;
import java.util.Map;
import ritzow.sandbox.client.graphics.ClientWorldRenderer;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.Control.Button;
import ritzow.sandbox.client.input.InputContext;
import ritzow.sandbox.client.input.controller.InteractionController;
import ritzow.sandbox.client.input.controller.PlayerController;
import ritzow.sandbox.client.input.controller.TrackingCameraController;
import ritzow.sandbox.client.network.UpdateClient;
import ritzow.sandbox.client.network.UpdateClient.ClientEvent;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;

class InWorldContext implements GameContext {
	UpdateClient client;
	ClientWorldRenderer worldRenderer;
	TrackingCameraController cameraGrip;
	PlayerController playerControls;
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
				//Map.entry(Control.MOVE_LEFT,  InWorldContext.this::leaveServer),
				//Map.entry(Control.MOVE_RIGHT,  InWorldContext.this::leaveServer),
				//Map.entry(Control.MOVE_UP,  InWorldContext.this::leaveServer),
				//Map.entry(Control.MOVE_DOWN,  InWorldContext.this::leaveServer)
		);

		@Override
		public Map<Button, Runnable> buttonControls() {
			return controls;
		}

		@Override
		public void keyboardButton(int key, int scancode, int action, int mods) {
			if(interactionControls != null)
				interactionControls.keyboardButton(key, scancode, action, mods);
			if(playerControls != null)
				playerControls.keyboardButton(key, scancode, action, mods);
			if(cameraGrip != null)
				cameraGrip.keyboardButton(key, scancode, action, mods);
		}

		@Override
		public void mouseButton(int button, int action, int mods) {
			//if(interactionControls != null)
			//	interactionControls.mouseButton(button, action, mods);
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
			updateNoPoll();
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
			client = UpdateClient.create(StartClient.localAddress, StartClient.serverAddress);
			client.setEventListener(ClientEvent.CONNECT_ACCEPTED, this::onAccepted);
			client.setEventListener(ClientEvent.CONNECT_REJECTED, this::onRejected);
			client.setEventListener(ClientEvent.DISCONNECTED, this::onDisconnected);
			client.setEventListener(ClientEvent.TIMED_OUT, this::onTimeout);
			client.setEventListener(ClientEvent.WORLD_JOIN, this::onWorldJoin);
			client.setEventListener(ClientEvent.EXCEPTION_OCCURRED, this::onException);
			client.beginConnect();
			GameLoop.setContext(client::update);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void updateGame() {
		StartClient.display.poll(input);
		client.update();
		long time = System.nanoTime();
		lastWorldUpdate = (StartClient.display.focused() && time - lastWorldUpdate < StartClient.UPDATE_SKIP_THRESHOLD_NANOSECONDS) ?
		Utility.updateWorld(world, lastWorldUpdate,
			Protocol.MAX_UPDATE_TIMESTEP, Protocol.TIME_SCALE_NANOSECONDS) : time; //simulate world on client
		long camUpdateStart = System.nanoTime();
		cameraGrip.update(player, StartClient.audio, camUpdateStart - lastCameraUpdate); //update camera position and zoom
		lastCameraUpdate = camUpdateStart;
		interactionControls.update(StartClient.display, cameraGrip.getCamera(), client,
				world, player, StartClient.display.width(), StartClient.display.height()); //block breaking/"bomb throwing"
		if(!StartClient.display.minimized()) RenderManager.run(StartClient.display, worldRenderer);
		Utility.sleep(1); //reduce CPU usage
	}

	private void updateNoPoll() {
		RenderManager.run(StartClient.display, worldRenderer);
		client.update();
	}

	private void onWorldJoin(World world, ClientPlayerEntity player) {
		this.world = world;
		this.player = player;
		cameraGrip = new TrackingCameraController(2.5f, 0.05f / player.getWidth(), 0.6f / player.getWidth());
		worldRenderer = new ClientWorldRenderer(StartClient.shaderProgram, cameraGrip.getCamera(), world);
		playerControls = new PlayerController(client);
		interactionControls = new InteractionController(client);
		StartClient.display.setCursor(StartClient.pickaxeCursor);
		lastWorldUpdate = System.nanoTime();
		lastCameraUpdate = System.nanoTime();
		GameLoop.setContext(this::updateGame);
	}

	private void onAccepted() {
		System.out.println("connected.");
	}

	private void onRejected() {
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