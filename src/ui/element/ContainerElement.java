package ui.element;

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

	@Override
	public float getWidth() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getHeight() {
		// TODO Auto-generated method stub
		return 0;
	}
}
