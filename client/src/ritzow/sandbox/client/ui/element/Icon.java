package ritzow.sandbox.client.ui.element;

import java.util.Collections;
import ritzow.sandbox.client.graphics.Graphics;
import ritzow.sandbox.client.graphics.ImmutableGraphics;
import ritzow.sandbox.client.graphics.RenderConstants;

public class Icon implements UIElement {
	private final Iterable<Graphics> appearance;

	public Icon() {
		appearance = Collections.singleton(new ImmutableGraphics(RenderConstants.MODEL_GREEN_FACE, 1.0f, 1.0f, 0.0f, 1.0f));
	}

	@Override
	public Iterable<Graphics> appearance() {
		return appearance;
	}

	@Override
	public void onHover(float localX, float localY) {

	}

	@Override
	public void onClick() {

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
