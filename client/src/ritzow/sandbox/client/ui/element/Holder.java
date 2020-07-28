package ritzow.sandbox.client.ui.element;

import java.util.Objects;
import ritzow.sandbox.client.ui.GuiElement;
import ritzow.sandbox.client.ui.GuiRenderer;
import ritzow.sandbox.client.ui.Rectangle;
import ritzow.sandbox.client.ui.Shape;

public class Holder<T extends GuiElement> implements GuiElement {
	private T element;

	public Holder(T element) {
		this.element = Objects.requireNonNull(element);
	}

	public T get() {
		return element;
	}

	public void set(T element) {
		this.element = Objects.requireNonNull(element);
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		if(element != null) {
			renderer.draw(element);
		}
	}

	@Override
	public Shape shape() {
		return element == null ? new Rectangle(0, 0) : element.shape();
	}
}
