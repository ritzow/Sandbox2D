package ritzow.sandbox.client.ui.element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import ritzow.sandbox.client.ui.GuiElement;
import ritzow.sandbox.client.ui.GuiRenderer;
import ritzow.sandbox.client.ui.Rectangle;

public class HBoxDynamic implements GuiElement {
	private final List<GuiElement> elements;
	private final float gap;

	public HBoxDynamic(float gap, GuiElement... elements) {
		this.elements = new ArrayList<>(Arrays.asList(elements));
		this.gap = gap;
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		Rectangle bounds = shape();
		float pos = -bounds.width()/2f;
		for(GuiElement e : elements) {
			Rectangle elementBounds = e.shape().toRectangle();
			pos += elementBounds.width() / 2;
			renderer.draw(e, pos, 0);
			pos += elementBounds.width() / 2 + gap;
		}
	}

	@Override
	public Rectangle shape() {
		float width = (elements.size() - 1) * gap, maxHeight = 0;
		for(GuiElement e : elements) {
			Rectangle rect = e.shape().toRectangle();
			width += rect.width();
			if(rect.height() > maxHeight) {
				maxHeight = rect.height();
			}
		}
		return new Rectangle(width, maxHeight);
	}
}
