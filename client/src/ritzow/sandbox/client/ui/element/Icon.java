package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.client.graphics.MutableGraphics;
import ritzow.sandbox.client.ui.GuiRenderer;

public class Icon implements GuiElement {
	private final MutableGraphics graphics;

	public Icon(Model modelID) {
		this.graphics = new MutableGraphics(modelID, 1.0f, 1.0f,  0/*Utility.randomAngleRadians()*/, 1.0f);
	}

	@Override
	public void update(long nanoseconds) {
		//graphics.rotation += (Math.PI/4 * nanoseconds/1_000_000_000f);
	}

	@Override
	public void render(GuiRenderer renderer) {
		renderer.draw(graphics.model, graphics.opacity, 0, 0, graphics.scaleX, graphics.scaleY, graphics.rotation);
	}
}
