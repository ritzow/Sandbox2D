package ritzow.sandbox.client;

import java.io.IOException;
import java.util.Map;
import ritzow.sandbox.client.graphics.Display;
import ritzow.sandbox.client.graphics.ModelRenderProgram;
import ritzow.sandbox.client.graphics.RenderConstants;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.Control.Button;
import ritzow.sandbox.client.input.InputContext;
import ritzow.sandbox.client.ui.UserInterface;
import ritzow.sandbox.client.ui.UserInterface.Position;
import ritzow.sandbox.client.ui.element.Icon;
import ritzow.sandbox.util.Utility;

class MainMenuContext {
	final UserInterface ui;

	MainMenuContext(ModelRenderProgram program) {
		this.ui = UserInterface.of(program,
			Map.entry(new Icon(RenderConstants.MODEL_RED_SQUARE), Position.of(0, 0)),
			Map.entry(new Icon(RenderConstants.MODEL_GRASS_BLOCK), Position.of(0.5f, 0)),
			Map.entry(new Icon(RenderConstants.MODEL_GREEN_FACE), Position.of(-0.5f, 0))
		);
		
		previousTime = System.nanoTime();
	}
	
	private long previousTime = 0;
	
	public void update() {
		refresh().poll(input);
		Utility.sleep(1); //reduce CPU usage TODO add frame rate limit
	}
	
	private Display refresh() {
		Display display = StartClient.display;
		int width = display.width(), height = display.height();
		long time = System.nanoTime();
		ui.update(display, time - previousTime);
		RenderManager.preRender(width, height);
		ui.render(RenderManager.DISPLAY_BUFFER, display.width(), display.height());
		previousTime = time;
		display.refresh();
		return display;
	}

	InputContext input = new InputContext() {

		@Override
		public void windowClose() {
			StartClient.exit();
		}

		@Override
		public void windowRefresh() {
			refresh();
		}

		private final Map<Button, Runnable> controls = Map.ofEntries(
			Map.entry(Control.FULLSCREEN, StartClient.display::toggleFullscreen),
			Map.entry(Control.QUIT,  StartClient::exit),
			Map.entry(Control.CONNECT, this::connect)
		);

		@Override
		public Map<Button, Runnable> buttonControls() {
			return controls;
		}
		
		private void connect() {
			GameLoop.setContext(() -> {
				try {
					new ServerJoinContext().listenForServer();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
	};
}