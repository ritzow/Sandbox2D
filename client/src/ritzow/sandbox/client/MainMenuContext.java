package ritzow.sandbox.client;

import java.util.Map;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.client.graphics.GameModels;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.input.Button;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.InputContext;
import ritzow.sandbox.client.ui.GuiElement;
import ritzow.sandbox.client.ui.StandardGuiRenderer;
import ritzow.sandbox.client.ui.element.AbsoluteGuiPositioner;
import ritzow.sandbox.client.ui.element.Text;

import static ritzow.sandbox.client.ui.element.AbsoluteGuiPositioner.alignCenter;
import static ritzow.sandbox.client.util.ClientUtility.log;

class MainMenuContext {
	private final StandardGuiRenderer ui;
	private final GuiElement root;
	private final InputContext context;
	private ServerJoinContext joinContext;

	MainMenuContext() {
		this.context = new MenuInputContext();
		this.ui = new StandardGuiRenderer(GameState.modelRenderer());
		root = new AbsoluteGuiPositioner(
			alignCenter(new ritzow.sandbox.client.ui.element.Button("press me!",
				GameModels.MODEL_GREEN_FACE, () -> log().info("you pressed me!")), 0, -0.5f), //TODO implement VBox (perhaps share implementation with HBox)
			//Map.entry(new Icon(GameModels.MODEL_GREEN_FACE), Position.of(-0.75f, -0.35f)),
			//Map.entry(new Icon(GameModels.MODEL_GREEN_FACE), Position.of(0.75f, -0.35f)),
			alignCenter(new Text("Hello World!", RenderManager.FONT, 15, 0), 0, 0.5f)
//			Map.entry(new RotationAnimation(new Scaler(new AbsoluteGuiPositioner(
//
//				//Map.entry(new Icon(GameModels.MODEL_RED_SQUARE), Position.of(0, 0)),
//				//Map.entry(new Text("blah", RenderManager.FONT, Layout.CENTERED, 10, 0), Position.of(0, 0))
//			), 0.5f), Utility.degreesPerSecToRadiansPerNano(0)), Position.of(-0.5f, -0.5f))
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
		RenderManager.preRender(width, height);
		ui.render(root, RenderManager.DISPLAY_BUFFER, GameState.display(), deltaTime, StandardClientOptions.GUI_SCALE);
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