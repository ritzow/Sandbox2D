package graphics.ui.element;

import graphics.Model;
import world.entity.component.visual.Graphics;

public class ModelElement extends InterfaceElement {

	public ModelElement(Model model) {
		this.graphics = new Graphics(model);
	}
	
	@Override
	public boolean isWorldElement() {
		return false;
	}

}
