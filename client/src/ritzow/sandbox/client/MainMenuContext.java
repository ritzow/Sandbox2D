package ritzow.sandbox.client;

import java.io.IOException;
import java.util.Map;
import ritzow.sandbox.client.graphics.ModelRenderProgram;
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
			Map.entry(new Icon(), Position.of(0, 0))
		);
	}
	
	public void update() {
		ui.update(StartClient.display);
		RenderManager.run(StartClient.display, ui);
		StartClient.display.poll(input);
		Utility.sleep(1); //reduce CPU usage
	}

	InputContext input = new InputContext() {

		@Override
		public void windowClose() {
			StartClient.exit();
		}

		@Override
		public void windowRefresh() {
			RenderManager.run(StartClient.display, ui);
		}

		private final Map<Button, Runnable> controls = Map.ofEntries(
				Map.entry(Control.FULLSCREEN, StartClient.display::toggleFullscreen),
				Map.entry(Control.QUIT,  StartClient::exit),
				Map.entry(Control.CONNECT, () -> GameLoop.setContext(() -> {
					try {
						new ServerJoinContext().listenForServer();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}))
				//Map.entry(Control.ACTIVATE_UI_ELEMENT, () -> ui.notifyClick(StartClient.display))
		);

		@Override
		public Map<Button, Runnable> buttonControls() {
			return controls;
		}
	};
}