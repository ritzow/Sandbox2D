package ritzow.sandbox.client;

import ritzow.sandbox.client.StartClient.GameLoop;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.graphics.Renderer;
import ritzow.sandbox.client.input.ControlScheme;
import ritzow.sandbox.client.input.InputContext;
import ritzow.sandbox.util.Utility;

class MainMenuContext implements GameContext {
	Renderer menuRenderer;
	
	MainMenuContext(Renderer menuRenderer) {
		this.menuRenderer = menuRenderer;
	}
	
	InputContext input = new InputContext() {
		
		public void windowClose() {
			StartClient.exit();
		}
		
		@Override
		public void keyboardButton(int key, int scancode, int action, int mods) {
			if(action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
				switch(key) {
					case ControlScheme.KEYBIND_CONNECT -> {
						try {
							GameLoop.start(new InGameContext());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					case ControlScheme.KEYBIND_QUIT -> StartClient.exit();
					case ControlScheme.KEYBIND_FULLSCREEN -> StartClient.display.toggleFullscreen();
				}
			}
		}
	};
	
	@Override
	public void run() {
		StartClient.display.poll(input);
		if(!StartClient.quit) {
			RenderManager.run(StartClient.display, menuRenderer);
			Utility.sleep(1); //reduce CPU usage	
		} else {
			StartClient.exit();
		}
	}

	@Override
	public String name() {
		return "Main Menu";
	}
}