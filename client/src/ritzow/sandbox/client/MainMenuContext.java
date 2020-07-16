package ritzow.sandbox.client;

import java.util.Map;
import ritzow.sandbox.client.graphics.GameModels;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.input.Button;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.InputContext;
import ritzow.sandbox.client.ui.Position;
import ritzow.sandbox.client.ui.StandardGuiRenderer;
import ritzow.sandbox.client.ui.element.AbsoluteGuiPositioner;
import ritzow.sandbox.client.ui.element.GuiElement;
import ritzow.sandbox.client.ui.element.Icon;

class MainMenuContext {
	private final StandardGuiRenderer ui;
	private final GuiElement root;
	private final InputContext context;
	private ServerJoinContext joinContext;

	MainMenuContext() {
		this.context = new MenuInputContext();
		this.ui = new StandardGuiRenderer(GameState.shader());
		root = new AbsoluteGuiPositioner(
			Map.entry(new Icon(GameModels.MODEL_GREEN_FACE), Position.of(-0.5f, 0)),
			Map.entry(new Icon(GameModels.MODEL_GREEN_FACE), Position.of(0.5f, 0))
		);
	}

	public void returnToMenu() {
		joinContext = null;
		GameLoop.setContext(this::update);
	}

	public void update(long delta) {
		GameState.display().poll(context);
		refresh(delta);
	}

	private void refresh(long deltaTime) {
		int width = GameState.display().width();
		int height = GameState.display().height();
		root.update(deltaTime);
		RenderManager.preRender(width, height);
		ui.render(root, RenderManager.DISPLAY_BUFFER, width, height);
		GameState.shader().flush();
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

	private final class MenuInputContext implements InputContext {
		@Override
		public void windowClose() {
			GameLoop.stop();
		}

		@Override
		public void windowRefresh() {
			refresh(GameLoop.updateDeltaTime());
		}

		private final Map<Button, Runnable> controls = Map.ofEntries(
			Map.entry(Control.FULLSCREEN, GameState.display()::toggleFullscreen),
			Map.entry(Control.QUIT, GameLoop::stop),
			Map.entry(Control.CONNECT, MainMenuContext.this::startJoin)
		);

		@Override
		public Map<Button, Runnable> buttonControls() {
			return controls;
		}
	}
}