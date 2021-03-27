package ritzow.sandbox.client;

import java.io.IOException;
import java.net.BindException;
import java.net.PortUnreachableException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.logging.Level;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.client.graphics.GameModels;
import ritzow.sandbox.client.graphics.GraphicsUtility;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.input.ControlsContext;
import ritzow.sandbox.client.network.Client;
import ritzow.sandbox.client.ui.GuiElement;
import ritzow.sandbox.client.ui.StandardGuiRenderer;
import ritzow.sandbox.client.ui.element.*;
import ritzow.sandbox.client.ui.element.BorderAnchor.Anchor;
import ritzow.sandbox.client.ui.element.BorderAnchor.Side;
import ritzow.sandbox.client.ui.element.VBoxDynamic.Alignment;
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
	private final EditableText text;

	private Client client;
	private InWorldContext worldContext;

	float uiScale = StandardClientOptions.GUI_SCALE;

	private final ControlsContext controlsContext = new ControlsContext(
		UI_ACTIVATE,
		UI_CONTEXT,
		UI_TERTIARY,
		UI_BACKSPACE,
		UI_DELETE,
		UI_CONFIRM,
		FULLSCREEN,
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

		@Override
		public void onCharacterTyped(int codePoint) {
			text.append(codePoint);
		}

		@Override
		public void onControlPressed(ritzow.sandbox.client.input.Button button) {
			onControlPressedRepeated(button);
		}

		@Override
		public void onControlRepeated(ritzow.sandbox.client.input.Button button) {
			onControlPressedRepeated(button);
		}

		private void onControlPressedRepeated(ritzow.sandbox.client.input.Button button) {
			if(text.hasContent()) {
				if(button.equals(UI_BACKSPACE)) {
					text.delete();
				} else if(button.equals(UI_DELETE)) {
					text.delete();
				}
			}
		}
	};

	private final Map<ritzow.sandbox.client.input.Button, Runnable> controls = Map.ofEntries(
		Map.entry(FULLSCREEN, GameState.display()::toggleFullscreen),
		Map.entry(UI_CONFIRM, this::startJoin)
	);

	private static void onQuit() {
		GameLoop.setContext(StartClient::shutdown);
	}

	MainMenuContext() {
		GameState.setGuiRenderer(new StandardGuiRenderer(GameState.modelRenderer()));
		root = new BorderAnchor(
			new Anchor(new Scaler(new Icon(GameModels.MODEL_GREEN_FACE), 2.0f), Side.CENTER, 0, 0),
			new Anchor(new VBoxDynamic(0.1f, Alignment.LEFT,
				new Scaler(new Button("Join Server", GameModels.MODEL_GREEN_FACE, this::startJoin),0.5f),
				new Button("Quit", GameModels.MODEL_SKY, MainMenuContext::onQuit)
			), Side.LEFT, 0.1f, 0),
			new Anchor(new VBox(0.1f,
				new Text("Sandbox2D", RenderManager.FONT, 15, 0),
				new Text("by Solomon Ritzow", RenderManager.FONT, 7, 0)
			), Side.TOP, 0, 0.05f),
			new Anchor(new VBox(0.1f,
				serverLoadProgress = new Holder<>(new Text("Not loading", RenderManager.FONT, 7, 0)),
				new HBoxDynamic(0.1f,
					new Text("Server:", RenderManager.FONT, 10, 0),
					text = new EditableText(RenderManager.FONT, 10, 0).setContent(NetworkUtility.formatAddress(getServerAddress()))
				)
			), Side.BOTTOM, 0, 0.05f)
		);

		//Query query = Query.createFor(Service.fromName("_sandbox2D._udp"), Domain.LOCAL);
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
		RenderManager.DISPLAY_BUFFER.clear(161 / 256f, 252 / 256f, 156 / 256f, 1.0f);
		RenderManager.DISPLAY_BUFFER.setCurrent();
		RenderManager.setStandardBlending();
		GameState.guiRenderer().render(
			root,
			GameState.display(), controlsContext,
			deltaTime,
			uiScale
		);
		GameState.modelRenderer().flush();
		RenderManager.postRender(GameState.display());
		GraphicsUtility.checkErrors();
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
			//TODO performe asynchronous host name resolution
			log().info(() -> "Connecting to " + text.content() + " from " + NetworkUtility.formatAddress(getLocalAddress()));
			client = Client.create(getLocalAddress(), NetworkUtility.parseSocket(text.content(), Protocol.DEFAULT_SERVER_PORT))
				 .setOnTimeout(this::onTimeout)
				 .setOnException(this::onException)
				 .beginConnect();
			displayText("Joining server");
		} catch(BindException e) {
			log().log(Level.WARNING, "Bind error", e);
			displayText("Bind error: " + e.getMessage());
		} catch(IOException | RuntimeException e) {
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