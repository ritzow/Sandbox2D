package ritzow.sandbox.client.ui.element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import ritzow.sandbox.client.ui.GuiRenderer;
import ritzow.sandbox.client.ui.Position;

public class AbsoluteGuiPositioner implements GuiElement {
	private static record ElementPos(GuiElement element, Position pos) {}
	private final List<ElementPos> elements;

	@SafeVarargs
	public AbsoluteGuiPositioner(Entry<GuiElement, Position>... elements) {
		this.elements = new ArrayList<>(elements.length);
		for(var entry : elements) {
			this.elements.add(new ElementPos(entry.getKey(), entry.getValue()));
		}
	}

	@Override
	public void render(GuiRenderer renderer) {
		for(ElementPos element : elements) {
			renderer.draw(element.element, element.pos.x(), element.pos.y());
		}
	}

	@Override
	public void update(long nanoseconds) {
		for(ElementPos element : elements) {
			element.element.update(nanoseconds);
		}
	}
}
