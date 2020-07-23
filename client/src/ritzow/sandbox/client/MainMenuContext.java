package ritzow.sandbox.client;

import java.util.Map;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.client.graphics.GameModels;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.input.ControlsContext;
import ritzow.sandbox.client.ui.GuiElement;
import ritzow.sandbox.client.ui.StandardGuiRenderer;
import ritzow.sandbox.client.ui.element.*;
import ritzow.sandbox.client.ui.element.BorderAnchor.Anchor;
import ritzow.sandbox.client.ui.element.BorderAnchor.Side;

import static ritzow.sandbox.client.input.Control.*;

class MainMenuContext {
	private final GuiElement root;
	private ServerJoinContext joinContext;

	private final ControlsContext MAIN_MENU_CONTEXT = new ControlsContext(
		UI_ACTIVATE,
		UI_CONTEXT,
		UI_TERTIARY,
		QUIT,
		FULLSCREEN,
		CONNECT) {
		@Override
		public void windowClose() {
			GameLoop.stop();
		}

		@Override
		public void windowRefresh() {
			refresh(GameLoop.updateDeltaTime());
		}
	};

	private final Map<ritzow.sandbox.client.input.Button, Runnable> controls = Map.ofEntries(
		Map.entry(FULLSCREEN, GameState.display()::toggleFullscreen),
		Map.entry(QUIT, GameLoop::stop)
	);

	MainMenuContext() {
		GameState.setGuiRenderer(new StandardGuiRenderer(GameState.modelRenderer()));
		root = new BorderAnchor(
			new Anchor(new VBox(0.1f,
				new Scaler(new Button("Join Server", GameModels.MODEL_GREEN_FACE, MainMenuContext.this::startJoin),0.5f),
				new Button("Quit", GameModels.MODEL_SKY, GameLoop::stop)
			), Side.LEFT, 0.1f, 0),
			new Anchor(new Text("Sandbox2D", RenderManager.FONT, 15, -0.03f), Side.TOP, 0, 0.05f),
			new Anchor(new Text(StandardClientOptions.getServerAddress().toString(), RenderManager.FONT, 7, 0), Side.BOTTOM, 0, 0.05f)
		);
	}

	public void returnToMenu() {
		joinContext = null;
		GameLoop.setContext(this::update);
	}

	public void update(long delta) {
		MAIN_MENU_CONTEXT.nextFrame();
		GameState.display().handleEvents(MAIN_MENU_CONTEXT);
		for(var entry : controls.entrySet()) {
			if(MAIN_MENU_CONTEXT.isNewlyPressed(entry.getKey())) {
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
			GameState.display(), MAIN_MENU_CONTEXT,
			deltaTime,
			StandardClientOptions.GUI_SCALE
		);
		GameState.modelRenderer().flush();
		RenderManager.postRender();
		GameState.display().refresh();
		if(joinContext != null) {
			if(joinContext.hasFailed()) {
				joinContext = null;
			} else {
				joinContext.listen();
			}
		}
	}

	private void startJoin() {
		joinContext = new ServerJoinContext();
	}
}