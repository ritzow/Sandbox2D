package ritzow.sandbox.client;

import java.io.IOException;
import java.util.Map;
import ritzow.sandbox.client.graphics.Display;
import ritzow.sandbox.client.graphics.RenderConstants;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.Control.Button;
import ritzow.sandbox.client.input.InputContext;
import ritzow.sandbox.client.ui.UserInterface;
import ritzow.sandbox.client.ui.UserInterface.Position;
import ritzow.sandbox.client.ui.element.Icon;
import ritzow.sandbox.client.util.ClientUtility;

class MainMenuContext {
	private final GameState state;
	private final UserInterface ui;
	private final InputContext context;
	private long previousTime;

	MainMenuContext(GameState state) {
		this.state = state;
		this.context = new MenuInputContext();
		this.ui = UserInterface.of(state.shader,
			Map.entry(new Icon(RenderConstants.MODEL_GREEN_FACE), Position.of(0, 0))
		);
		
		previousTime = System.nanoTime();
	}
	
	public void update() {
		long frameStart = System.nanoTime();
		state.display.poll(context);
		refresh();
		ClientUtility.limitFramerate(frameStart);
	}
	
	private void refresh() {
		Display display = state.display;
		int width = display.width(), height = display.height();
		long time = System.nanoTime();
		ui.update(display, time - previousTime);
		RenderManager.preRender(width, height);
		ui.render(RenderManager.DISPLAY_BUFFER, width, height);
		previousTime = time;
		display.refresh();
	}
	
	private void startJoin() {
		try {
			state.menuContext = this;
			ServerJoinContext.start(state);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private final class MenuInputContext implements InputContext {

		@Override
		public void windowClose() {
			GameLoop.stop();
		}

		@Override
		public void windowRefresh() {
			refresh();
		}

		private final Map<Button, Runnable> controls = Map.ofEntries(
			Map.entry(Control.FULLSCREEN, state.display::toggleFullscreen),
			Map.entry(Control.QUIT, GameLoop::stop),
			Map.entry(Control.CONNECT, MainMenuContext.this::startJoin)
		);

		@Override
		public Map<Button, Runnable> buttonControls() {
			return controls;
		}
	}
}