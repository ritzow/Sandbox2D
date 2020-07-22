package ritzow.sandbox.client.ui.element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import ritzow.sandbox.client.ui.*;

public class AbsoluteGuiPositioner implements GuiElement {
	private static record ElementPos(GuiElement element, Position pos) {}
	private final List<ElementPos> elements;
	private final Rectangle bounds;

	@SafeVarargs
	public AbsoluteGuiPositioner(Entry<GuiElement, Position>... elements) {
		this.elements = new ArrayList<>(elements.length);
		float leftX = 0, rightX = 0, bottomY = 0, topY = 0;
		//float width = 0, height = 0;
		for(var entry : elements) {
			Rectangle rect = entry.getKey().shape().toRectangle();
			if(rect != null) {
				leftX = Math.min(entry.getValue().x() - rect.width()/2, leftX);
				rightX = Math.max(entry.getValue().x() + rect.width()/2, rightX);
				bottomY = Math.min(entry.getValue().y() - rect.height()/2, bottomY);
				topY = Math.max(entry.getValue().y() + rect.height()/2, topY);
				//width = Math.max(Math.abs(entry.getValue().y()) + rect.width()/2, width);
				//height = Math.max(Math.abs(entry.getValue().y()) + rect.height()/2, height);
			}
			this.elements.add(new ElementPos(entry.getKey(), entry.getValue()));
		}
		this.bounds = new Rectangle(Math.max(Math.abs(leftX), Math.abs(rightX)) * 2, Math.max(Math.abs(bottomY), Math.abs(topY)) * 2);
	}

	public static Entry<GuiElement, Position> alignLeft(GuiElement element, float x, float y) {
		if(element.shape().toRectangle() instanceof Rectangle r) {
			return Map.entry(element, Position.of(x + r.width()/2, 0));
		} else return Map.entry(element, Position.of(x, y));
	}

	public static Entry<GuiElement, Position> alignCenter(GuiElement element, float x, float y) {
		return Map.entry(element, Position.of(x, y));
	}

	public static Entry<GuiElement, Position> alignRight(GuiElement element, float x, float y) {
		throw new UnsupportedOperationException("not implemented yet");
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		for(ElementPos element : elements) {
			renderer.draw(element.element, element.pos.x(), element.pos.y());
		}
	}

	@Override
	public Shape shape() {
		return bounds;
	}
}
