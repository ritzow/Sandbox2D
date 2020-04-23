package ritzow.sandbox.client.ui.element;

import java.util.Collections;
import ritzow.sandbox.client.graphics.Graphics;
import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.client.graphics.MutableGraphics;
import ritzow.sandbox.util.Utility;

public class Icon implements UIElement {
	private final Iterable<Graphics> appearance;
	private final MutableGraphics graphics;

	public Icon(Model modelID) {
		var graphics = new MutableGraphics(modelID, 1.0f, 1.0f, Utility.randomAngleRadians(), 1.0f);
		appearance = Collections.singleton(graphics);
		this.graphics = graphics;
	}

	@Override
	public Iterable<Graphics> appearance() {
		return appearance;
	}

	@Override
	public void update(long nanoseconds) {
		graphics.rotation += (Math.PI/4 * nanoseconds/1_000_000_000f);
	}

	@Override
	public void setOnClick(Runnable action) {

	}

	@Override
	public void onHover(float localX, float localY) {

	}

	@Override
	public float width() {
		return 1;
	}

	@Override
	public float height() {
		return 1;
	}

}
