package graphics.ui;

import graphics.ui.element.InterfaceElement;
import java.util.HashMap;

public class InterfaceManager {	
	protected final HashMap<InterfaceElement, DynamicLocation> elements;
	
	public InterfaceManager() {
		elements = new HashMap<InterfaceElement, DynamicLocation>();
	}
	
	public HashMap<InterfaceElement, DynamicLocation> getElements() {
		return elements;
	}
	
	public boolean add(InterfaceElement element) {
		return add(element, null);
	}
	
	public boolean add(InterfaceElement element, DynamicLocation location) {
		if(element.isWorldElement()) {
			return false;
		}
		
		else {
			elements.put(element, location);
			return true;
		}
	}
	
	public void remove(InterfaceElement element) {
		elements.remove(element);
	}
	
	public float getX(InterfaceElement element, DynamicLocation location, float frameWidth) {
		if(location == null) {
			if(element != null) {
				return element.position().getX();
			}
			
			else {
				return 0;
			}
		}
		
		frameWidth /= 2;

		float position = (((location.horizontal - 0) * (frameWidth - -frameWidth)) / (1 - 0)) + -frameWidth;

		if(position != 0.5f) {
			if(position > 0.5f) {
				return position - location.paddingX;
			}
			
			else if(position < 0.5f) {
				return position + location.paddingX;
			}
			
			else {
				return 0;
			}
		}
		
		else {
			return position;
		}
	}
	
	public float getY(InterfaceElement element, DynamicLocation location, float frameHeight) {
		if(location == null) {
			if(element != null) {
				return element.position().getY();
			}
			
			else {
				return 0;
			}
		}
		
		frameHeight /= 2;
		
		float position = (((location.vertical - 0) * (frameHeight - -frameHeight)) / (1 - 0)) + -frameHeight;
		
		if(position != 0.5f) {
			if(position > 0.5f) {
				return position - location.paddingY;
			}
			
			else if(position < 0.5f) {
				return position + location.paddingY;
			}
			
			else {
				return 0;
			}
		}
		
		else {
			return 0;
		}
	}
	
}
