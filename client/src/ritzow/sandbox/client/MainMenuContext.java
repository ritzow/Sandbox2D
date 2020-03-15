package ritzow.sandbox.client;

import java.util.Map;
import ritzow.sandbox.client.graphics.RenderConstants;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.Control.Button;
import ritzow.sandbox.client.input.InputContext;
import ritzow.sandbox.client.ui.UserInterface;
import ritzow.sandbox.client.ui.UserInterface.Position;
import ritzow.sandbox.client.ui.element.Icon;

class MainMenuContext {
	private final UserInterface ui;
	private final InputContext context;
	private ServerJoinContext joinContext;

	MainMenuContext() {
		this.context = new MenuInputContext();
		this.ui = UserInterface.of(GameState.shader(),
			Map.entry(new Icon(RenderConstants.MODEL_GREEN_FACE), Position.of(0, 0))
			//Map.entry(new Text("hello world", Fonts.getDefaultFont(), 1, 1), Position.of(0, 0))
		);
	}

	public void update(long delta) {
		GameState.display().poll(context);
		refresh(delta);
	}

	private void refresh(long deltaTime) {
		int width = GameState.display().width();
		int height = GameState.display().height();
		ui.update(GameState.display(), deltaTime);
		RenderManager.preRender(width, height);
		ui.render(RenderManager.DISPLAY_BUFFER, width, height);
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