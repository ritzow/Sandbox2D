package ritzow.sandbox.client;

import java.io.IOException;
import ritzow.sandbox.client.StartClient.GameLoop;
import ritzow.sandbox.client.graphics.ClientWorldRenderer;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.input.ControlScheme;
import ritzow.sandbox.client.input.InputContext;
import ritzow.sandbox.client.input.controller.InteractionController;
import ritzow.sandbox.client.input.controller.PlayerController;
import ritzow.sandbox.client.input.controller.TrackingCameraController;
import ritzow.sandbox.client.network.UpdateClient;
import ritzow.sandbox.client.network.UpdateClient.ClientEvent;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.network.TimeoutException;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;

class InGameContext implements GameContext {
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
			StartClient.exit();
		}
		
		@Override
		public void keyboardButton(int key, int scancode, int action, int mods) {
			if(action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
				switch(key) {
					case ControlScheme.KEYBIND_QUIT -> returnToMenu();
					case ControlScheme.KEYBIND_FULLSCREEN -> StartClient.display.toggleFullscreen();
				}
			}
			
			if(interactionControls != null)
				interactionControls.keyboardButton(key, scancode, action, mods);
			if(playerControls != null)
				playerControls.keyboardButton(key, scancode, action, mods);
			if(cameraGrip != null)
				cameraGrip.keyboardButton(key, scancode, action, mods);
		}
		
		@Override
		public void mouseButton(int button, int action, int mods) {
			if(interactionControls != null)
				interactionControls.mouseButton(button, action, mods);
			if(cameraGrip != null)
				cameraGrip.mouseButton(button, action, mods);
		}
		
		@Override
		public void mouseScroll(double xoffset, double yoffset) {
			if(cameraGrip != null)
				cameraGrip.mouseScroll(xoffset, yoffset);		
		}
		
		@Override
		public void cursorPos(double xpos, double ypos) {
			if(interactionControls != null)
				interactionControls.cursorPos(xpos, ypos);
		}
	};
	
	@Override
	public void run() {
		System.out.print("Connecting to " + Utility.formatAddress(StartClient.serverAddress)
		+ " from " + Utility.formatAddress(StartClient.localAddress) + "... ");
		try {
			client = UpdateClient.startConnection(StartClient.localAddress, StartClient.serverAddress);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		client.setEventListener(ClientEvent.CONNECT_ACCEPTED, this::onAccepted);
		client.setEventListener(ClientEvent.CONNECT_REJECTED, this::onRejected);
		client.setEventListener(ClientEvent.DISCONNECTED, this::onDisconnected);
		client.setEventListener(ClientEvent.WORLD_JOIN, this::setupGameWorld);
		GameLoop.setContext(() -> {
			try {
				client.update();
			} catch (TimeoutException | IOException e) {
				e.printStackTrace();
			}
		});
	}
	
	@Override
	public String name() {
		return "Multiplayer";
	}
	
	public void returnToMenu() {
		client.startDisconnect();
		GameLoop.setContext(() -> {
			try {
				client.update();
			} catch (TimeoutException | IOException e) {
				e.printStackTrace();
			}
		});
	}
	
	private void setupGameWorld(World world, ClientPlayerEntity player) {
		this.world = world;
		this.player = player;
		cameraGrip = new TrackingCameraController(StartClient.audio, player, 2.5f, 0.05f, 0.6f);
		worldRenderer = new ClientWorldRenderer(StartClient.shaderProgram, cameraGrip.getCamera(), world);
		playerControls = new PlayerController(client);
		interactionControls = new InteractionController(client);
		StartClient.display.setCursor(StartClient.pickaxeCursor);
		lastWorldUpdate = System.nanoTime();
		lastCameraUpdate = System.nanoTime();
		GameLoop.start(() -> {
			try {
				updateGame();
			} catch (TimeoutException | IOException e) {
				e.printStackTrace();
			}
		});
		StartClient.display.resetCursor();
	}
	
	void updateGame() throws TimeoutException, IOException {
		StartClient.display.poll(input);
		client.update();
		long time = System.nanoTime();
		lastWorldUpdate = (StartClient.display.focused() && time - lastWorldUpdate < StartClient.UPDATE_SKIP_THRESHOLD_NANOSECONDS) ?
		Utility.updateWorld(world, lastWorldUpdate, 
			Protocol.MAX_UPDATE_TIMESTEP, Protocol.TIME_SCALE_NANOSECONDS) : time; //simulate world on client
		long camUpdateStart = System.nanoTime();
		cameraGrip.update(camUpdateStart - lastCameraUpdate); //update camera position and zoom
		lastCameraUpdate = camUpdateStart;
		interactionControls.update(cameraGrip.getCamera(), client, 
				world, player, StartClient.display.width(), StartClient.display.height()); //block breaking/"bomb throwing"
		if(!StartClient.display.minimized()) RenderManager.run(StartClient.display, worldRenderer);
		Utility.sleep(1); //reduce CPU usage
	}
	
	private void onAccepted() {
		System.out.println("connected.");
	}
	
	private void onRejected() {
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		GameLoop.setContext(StartClient.mainMenu);
	}
	
	private void onDisconnected() {
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(StartClient.display.wasClosed()) { //TODO will cause GameLoop problems
			StartClient.exit();
		} else {
			GameLoop.setContext(StartClient.mainMenu);	
		}
	}
}