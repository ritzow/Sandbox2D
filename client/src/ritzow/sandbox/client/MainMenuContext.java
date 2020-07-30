package ritzow.sandbox.client;

import java.io.IOException;
import java.net.BindException;
import java.net.PortUnreachableException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.logging.Level;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.client.graphics.GameModels;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.input.ControlsContext;
import ritzow.sandbox.client.network.Client;
import ritzow.sandbox.client.ui.GuiElement;
import ritzow.sandbox.client.ui.StandardGuiRenderer;
import ritzow.sandbox.client.ui.element.*;
import ritzow.sandbox.client.ui.element.BorderAnchor.Anchor;
import ritzow.sandbox.client.ui.element.BorderAnchor.Side;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.Protocol;

import static ritzow.sandbox.client.data.StandardClientOptions.getLocalAddress;
import static ritzow.sandbox.client.data.StandardClientOptions.getServerAddress;
import static ritzow.sandbox.client.input.Control.*;
import static ritzow.sandbox.client.util.ClientUtility.log;
import static ritzow.sandbox.network.Protocol.TYPE_SERVER_CONNECT_ACKNOWLEDGMENT;

class MainMenuContext {
	private final GuiElement root;
	private final Holder<Text> serverLoadProgress;

	private Client client;
	private InWorldContext worldContext;

	float uiScale = StandardClientOptions.GUI_SCALE;

	private final ControlsContext controlsContext = new ControlsContext(
		UI_ACTIVATE,
		UI_CONTEXT,
		UI_TERTIARY,
		QUIT,
		FULLSCREEN,
		CONNECT,
		SCROLL_MODIFIER) {

		@Override
		public void windowClose() {
			onQuit();
		}

		@Override
		public void windowRefresh() {
			refresh(GameLoop.updateDeltaTime());
		}

		@Override
		public void mouseScroll(double horizontal, double vertical) {
			if(controlsContext.isPressed(SCROLL_MODIFIER)) {
				uiScale += vertical * 20;
			}
		}
	};

	private final Map<ritzow.sandbox.client.input.Button, Runnable> controls = Map.ofEntries(
		Map.entry(FULLSCREEN, GameState.display()::toggleFullscreen),
		Map.entry(QUIT, MainMenuContext::onQuit)
	);

	private static void onQuit() {
		GameLoop.setContext(StartClient::shutdown);
	}

	MainMenuContext() {
		GameState.setGuiRenderer(new StandardGuiRenderer(GameState.modelRenderer()));
		root = new BorderAnchor(
			new Anchor(new Scaler(new Icon(GameModels.MODEL_GREEN_FACE), 2.0f), Side.CENTER, 0, 0),
			new Anchor(new VBox(0.1f,
				new Scaler(new Button("Join Server", GameModels.MODEL_GREEN_FACE, this::startJoin),0.5f),
				new Button("Quit", GameModels.MODEL_SKY, MainMenuContext::onQuit)
			), Side.LEFT, 0.1f, 0),
			new Anchor(new VBox(0.1f,
				new Text("Sandbox2D", RenderManager.FONT, 15, 0f),
				new Text("by Solomon Ritzow", RenderManager.FONT, 7, 0)
			), Side.TOP, 0, 0.05f),
			new Anchor(new VBox(0.1f,
				serverLoadProgress = new Holder<>(new Text("Not loading", RenderManager.FONT, 7, 0)),
				new Text(getServerAddress().toString(), RenderManager.FONT, 7, 0)
			), Side.BOTTOM, 0, 0.05f),
			new Anchor(new Scaler(new Icon(GameModels.MODEL_ATLAS), 0.75f), Side.BOTTOM_RIGHT, 0.05f, 0.05f)
		);
	}

	public void onLeaveWorld() {
		client = null;
		worldContext = null;
		GameLoop.setContext(this::update);
	}

	public void update(long delta) {
		GameState.display().handleEvents(controlsContext);
		for(var entry : controls.entrySet()) {
			if(controlsContext.isNewlyPressed(entry.getKey())) {
				entry.getValue().run();
			}
		}
		refresh(delta);
	}

	private void refresh(long deltaTime) {
		int width = GameState.display().width();
		int height = GameState.display().height();
		RenderManager.preRender(width, height);
		RenderManager.DISPLAY_BUFFER.clear(161 / 256f, 252 / 256f, 156 / 256f, 1.0f);
		GameState.guiRenderer().render(
			root,
			RenderManager.DISPLAY_BUFFER,
			GameState.display(), controlsContext,
			deltaTime,
			uiScale
		);
		GameState.modelRenderer().flush();
		RenderManager.postRender();
		GameState.display().refresh();
		if(client != null) {
			if(worldContext == null) {
				client.update(this::process);
			} else {
				worldContext.updateJoining();
			}
		}
	}

	private void startJoin() {
		try {
			log().info(() -> "Connecting to " + NetworkUtility.formatAddress(getServerAddress())
				 + " from " + NetworkUtility.formatAddress(getLocalAddress()));
			client = Client.create(getLocalAddress(), getServerAddress())
				 .setOnTimeout(this::onTimeout)
				 .setOnException(this::onException)
				 .beginConnect();
			displayText("Joining server");
		} catch(BindException e) {
			log().log(Level.WARNING, "Bind error", e);
			displayText("Bind error: " + e.getMessage());
		} catch(IOException e) {
			displayText("Error: " + e.getMessage());
		}
	}

	private void close() {
		try {
			client.close();
			client = null;
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void onTimeout() {
		log().warning("Connection timed out.");
		displayText("Connection timed out");
		close();
	}

	private void onException(IOException e) {
		if(e instanceof PortUnreachableException) {
			log().info("Server port unreachable.");
			displayText("Server port unreachable");
		} else {
			log().log(Level.SEVERE, "An IOException occurred while joining server", e);
			displayText("Error: " + e.getMessage());
		}
		close();
	}

	private void displayText(String text) {
		serverLoadProgress.set(new Text(text, RenderManager.FONT, 7, 0));
	}

	private boolean process(ByteBuffer data) {
		short messageType = data.getShort();
		if(messageType == TYPE_SERVER_CONNECT_ACKNOWLEDGMENT) {
			byte response = data.get();
			switch(response) {
				case Protocol.CONNECT_STATUS_REJECTED -> {
					log().info("Server rejected connection");
					close();
					displayText("Server rejected connection");
					return false;
				}

				case Protocol.CONNECT_STATUS_WORLD -> {
					log().info("Connected to server");
					//worldSize and playerID integers
					worldContext = new InWorldContext(client, data.getInt(), data.getInt(), progress -> {
						if(progress < 1.0) {
							displayText("Loading " + (int)(progress * 100) + "%");
						} else displayText("Building world");
					});
					return false;
				}

				default -> throw new UnsupportedOperationException("Unsupported connect ack type " + response);
			}
		} else {
			throw new UnsupportedOperationException(messageType + " not supported during connect");
		}
	}
}