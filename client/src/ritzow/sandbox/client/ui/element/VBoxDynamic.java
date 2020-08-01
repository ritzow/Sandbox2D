package ritzow.sandbox.client.ui.element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import ritzow.sandbox.client.ui.GuiElement;
import ritzow.sandbox.client.ui.GuiRenderer;
import ritzow.sandbox.client.ui.Rectangle;

public class VBoxDynamic implements GuiElement {
	private final List<GuiElement> elements;
	private final float gap;
	private final Alignment alignment;

	public enum Alignment {
		LEFT {
			@Override
			float aligned(float maxWidth, float width) {
				return width/2f - maxWidth/2f;
			}
		},
		CENTER {
			@Override
			float aligned(float maxWidth, float width) {
				return 0;
			}
		},
		RIGHT {
			@Override
			float aligned(float maxWidth, float width) {
				return maxWidth/2f - width/2f;
			}
		};

		abstract float aligned(float maxWidth, float width);
	}

	public VBoxDynamic(float gap, Alignment alignment, GuiElement... elements) {
		this.elements = new ArrayList<>(Arrays.asList(elements));
		this.gap = gap;
		this.alignment = alignment;
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		Rectangle bounds = shape();
		float pos = bounds.height()/2f;
		for(GuiElement e : elements) {
			Rectangle elementBounds = e.shape().toRectangle();
			pos -= elementBounds.height() / 2;
			renderer.draw(e, alignment.aligned(bounds.width(), elementBounds.width()), pos);
			pos -= elementBounds.height() / 2 + gap;
		}
	}

	public boolean add(GuiElement element) {
		return elements.add(element);
	}

	public boolean remove(GuiElement element) {
		return elements.remove(element);
	}

	@Override
	public Rectangle shape() {
		float height = (elements.size() - 1) * gap, maxWidth = 0;
		for(GuiElement e : elements) {
			Rectangle rect = e.shape().toRectangle();
			height += rect.height();
			if(rect.width() > maxWidth) {
				maxWidth = rect.width();
			}
		}
		return new Rectangle(maxWidth, height);
	}
}
