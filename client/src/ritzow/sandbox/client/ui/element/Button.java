package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.ui.GuiElement;
import ritzow.sandbox.client.ui.GuiRenderer;
import ritzow.sandbox.client.ui.Shape;

public class Button implements GuiElement {
	private final GuiElement content;
	private final Runnable action;

	public Button(GuiElement content, Runnable action) {
		this.content = content;
		this.action = action;
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		renderer.draw(content, 0, 0);
	}

	@Override
	public boolean render(GuiRenderer renderer, long nanos, float mouseX, float mouseY) {
		renderer.draw(content, 0, 0);
		//action.run();
		//TODO implement button state, pressing, releasing, press indicator (separate content?)
		return true;
	}

	@Override
	public Shape shape() {
		return content.shape();
	}
}
