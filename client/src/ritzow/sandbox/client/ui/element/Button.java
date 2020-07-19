package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.ui.GuiElement;
import ritzow.sandbox.client.ui.GuiRenderer;
import ritzow.sandbox.client.ui.Shape;

public class Button implements GuiElement {
	private final GuiElement content;
	private final Runnable action;

	private static final float HOVER_SCALE = 1.25f;

	public Button(GuiElement content, Runnable action) {
		this.content = content;
		this.action = action;
	}

	public Button(String text, Model model, Runnable action) {
		this.content = new HBox(0.2f, new Icon(model), new Text(text, RenderManager.FONT, 12, 0));
		this.action = action;
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		renderer.draw(content, 0, 0);
	}

	@Override
	public boolean render(GuiRenderer renderer, long nanos, float mouseX, float mouseY) {
		renderer.draw(content, 1.0f, 0, 0, HOVER_SCALE, HOVER_SCALE, 0);
		//action.run();
		//TODO implement button state, pressing, releasing, press indicator (separate content?)
		return true;
	}

	@Override
	public Shape shape() {
		return content.shape();
	}
}
