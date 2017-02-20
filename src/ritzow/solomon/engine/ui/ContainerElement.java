package ritzow.solomon.engine.ui;

import java.util.ArrayList;
import java.util.List;
import ritzow.solomon.engine.graphics.ModelRenderer;

public class ContainerElement extends Element {
	protected final List<Element> elements;
	
	public ContainerElement() {
		elements = new ArrayList<Element>();
	}
	
	public List<Element> getElements() {
		return elements;
	}

	@Override
	public void render(ModelRenderer renderer) {
		for(Element e : elements) {
			e.render(renderer);
		}
	}

	@Override
	public float getWidth() {
		return 0;
	}

	@Override
	public float getHeight() {
		return 0;
	}
}
