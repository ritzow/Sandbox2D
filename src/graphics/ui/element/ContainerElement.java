package graphics.ui.element;

import graphics.Renderer;
import java.util.ArrayList;

public class ContainerElement extends Element {
	protected final ArrayList<Element> elements;
	
	{
		elements = new ArrayList<Element>();
	}
	
	public ArrayList<Element> getElements() {
		return elements;
	}

	@Override
	public void render(Renderer renderer, float x, float y) {
		for(Element e : elements) {
			e.render(renderer, x, y);
		}
	}
}
