package graphics.ui.element;

import java.util.ArrayList;
import world.entity.component.spacial.Position;
import world.entity.component.visual.Graphics;

public abstract class InterfaceElement {
	protected Position position;
	protected Graphics graphics;
	
	protected ArrayList<InterfaceElement> children;
	
	{
		position = new Position(0,0);
		children = new ArrayList<InterfaceElement>();
	}
	
	public Graphics graphics() {
		return graphics;
	}
	
	public Position position() {
		return position;
	}
	
	public ArrayList<InterfaceElement> getChildren() {
		return children;
	}
	
	public boolean hasChildren() {
		return children != null && children.size() > 0;
	}
	
	public abstract boolean isWorldElement();
	
}
