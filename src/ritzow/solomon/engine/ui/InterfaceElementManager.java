package ritzow.solomon.engine.ui;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import java.util.HashMap;
import java.util.Map.Entry;
import ritzow.solomon.engine.graphics.ModelRenderer;
import ritzow.solomon.engine.input.InputManager;
import ritzow.solomon.engine.input.handler.CursorPosHandler;
import ritzow.solomon.engine.input.handler.FramebufferSizeHandler;
import ritzow.solomon.engine.input.handler.MouseButtonHandler;
import ritzow.solomon.engine.ui.element.Element;
import ritzow.solomon.engine.ui.element.button.Button;
import ritzow.solomon.engine.util.Utility.Intersection;

public class InterfaceElementManager extends ElementManager implements CursorPosHandler, MouseButtonHandler, FramebufferSizeHandler {
	protected volatile float cursorX, cursorY;
	protected volatile float oldFramebufferWidth, oldFramebufferHeight;
	protected volatile float framebufferWidth, framebufferHeight;
	protected volatile boolean mouseDown;
	protected volatile boolean updatePositions;
	
	protected HashMap<Element, DynamicLocation> elements;
	//TODO fix element manager
	public InterfaceElementManager() {
		this.elements = new HashMap<Element, DynamicLocation>();
	}
	
	@Override
	public void render(ModelRenderer renderer) {
		renderer.loadViewMatrix(false);
		for(Entry<Element, DynamicLocation> entry : elements.entrySet()) {
			entry.getKey().render(renderer);
		}
	}
	
	@Override
	public void update() {
		update(framebufferWidth, framebufferHeight, cursorX, cursorY, mouseDown);
	}
	
	public void add(Element element, DynamicLocation location) {
		elements.put(element, location);
	}
	
	public void remove(Element element) {
		elements.remove(element);
	}
	
	protected void update(float framebufferWidth, float framebufferHeight, float mouseX, float mouseY, boolean mouseDown) {
		mouseX = (2f * mouseX) / framebufferWidth - 1f;
		mouseX /= (framebufferWidth != 0 ? framebufferHeight/framebufferWidth : 1);
		mouseY = -((2f * mouseY) / framebufferHeight - 1f);

		for(Entry<Element, DynamicLocation> entry : elements.entrySet()) {
			if(updatePositions) {
				//entry.getKey().setPositionX(getX(entry.getValue(), framebufferHeight/framebufferWidth));
				//entry.getKey().setPositionY(getY(entry.getValue()));
			}
			
			if(entry.getKey() instanceof Button && entry.getValue() != null) {
				Button button = (Button)entry.getKey();
				boolean hovering = Intersection.intersection(entry.getKey().getPositionX(), entry.getKey().getPositionY(), entry.getKey().getWidth(), entry.getKey().getHeight(), mouseX, mouseY);
				
				if(button.getHovered() && !hovering) 
					button.onUnHover();
				else if(!button.getHovered() && hovering) 
					button.onHover();
				
				if(hovering && mouseDown && !button.getPressed()) 
					button.onPress();
				else if(!mouseDown && button.getPressed()) 
					button.onRelease();
			}
		}
		updatePositions = false;
	}
	
	protected void updateElement(Element e, float framebufferWidth, float framebufferHeight) {
		
	}
	
//	private static float getX(DynamicLocation location, float aspectRatio) {
//		float position = location.horizontal/aspectRatio;
//		if(position > 0) return position - location.paddingX;
//		else if(position < 0) return position + location.paddingX;
//		else return position;
//	}
//	
//	private static float getY(DynamicLocation location) {
//		if(location.vertical > 0) 
//			return location.vertical - location.paddingY;
//		else if(location.vertical < 0) 
//			return location.vertical + location.paddingY;
//		else 
//			return location.vertical;
//	}

	@Override
	public void cursorPos(double xpos, double ypos) {
		this.cursorX = (float)xpos;
		this.cursorY = (float)ypos;
	}
	
	@Override
	public void mouseButton(int button, int action, int mods) {
		if(button == GLFW_MOUSE_BUTTON_LEFT) {
			if(action == GLFW_PRESS) {
				mouseDown = true;
			}
			
			else if(action == GLFW_RELEASE) {
				mouseDown = false;
			}
		}
	}

	@Override
	public void framebufferSize(int width, int height) {
		framebufferWidth = width;
		framebufferHeight = height;
		updatePositions = true;
	}

	@Override
	public void link(InputManager manager) {
		manager.getCursorPosHandlers().add(this);
		manager.getFramebufferSizeHandlers().add(this);
		manager.getMouseButtonHandlers().add(this);
	}

	@Override
	public void unlink(InputManager manager) {
		manager.getCursorPosHandlers().remove(this);
		manager.getFramebufferSizeHandlers().remove(this);
		manager.getMouseButtonHandlers().remove(this);
	}
}
