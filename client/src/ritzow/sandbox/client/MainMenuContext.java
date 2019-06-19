package ritzow.sandbox.client;

import java.util.Map;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.graphics.Renderer;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.Control.Button;
import ritzow.sandbox.client.input.InputContext;
import ritzow.sandbox.util.Utility;

class MainMenuContext implements GameContext {
	Renderer menuRenderer;

	MainMenuContext(Renderer menuRenderer) {
		this.menuRenderer = menuRenderer;
	}

	InputContext input = new InputContext() {

		@Override
		public void windowClose() {
			StartClient.exit();
		}

		@Override
		public void windowRefresh() {
			RenderManager.run(StartClient.display, menuRenderer);
		};

		private final Map<Button, Runnable> controls = Map.ofEntries(
				Map.entry(Control.FULLSCREEN, StartClient.display::toggleFullscreen),
				Map.entry(Control.QUIT,  StartClient::exit),
				Map.entry(Control.CONNECT, () -> GameLoop.setContext(new InWorldContext()))
		);

		@Override
		public Map<Button, Runnable> buttonControls() {
			return controls;
		}
	};

	@Override
	public void run() {
		Utility.sleep(1); //reduce CPU usage
		RenderManager.run(StartClient.display, menuRenderer);
		StartClient.display.poll(input);
	}
}