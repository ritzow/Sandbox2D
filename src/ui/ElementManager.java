package ui;

import graphics.Renderable;
import graphics.ModelRenderer;
import input.InputManager;
import input.handler.CursorPosHandler;
import input.handler.FramebufferSizeHandler;
import input.handler.MouseButtonHandler;
import java.util.HashMap;
import ui.element.Element;
import ui.element.button.Button;
import util.HitboxUtil;
import util.Updatable;

import static org.lwjgl.glfw.GLFW.*;

public final class ElementManager extends HashMap<Element, DynamicLocation> implements Renderable, Updatable, CursorPosHandler, MouseButtonHandler, FramebufferSizeHandler {
	private static final long serialVersionUID = 1509124666145321593L;
	
	protected volatile float cursorX;
	protected volatile float cursorY;
	protected volatile float framebufferWidth;
	protected volatile float framebufferHeight;
	protected volatile boolean mouseDown;
	private boolean updatePositions;
	
	@Override
	public void render(ModelRenderer renderer) {
		renderer.loadViewMatrix(false);
		for(Entry<Element, DynamicLocation> entry : entrySet()) {
			if(entry.getValue() != null) {
				entry.getKey().render(renderer, entry.getKey().getPositionX(), entry.getKey().getPositionY());
			}
		}
	}
	
	public void update() {
		update(framebufferWidth, framebufferHeight, cursorX, cursorY, mouseDown);
	}
	
	private void update(float framebufferWidth, float framebufferHeight, float mouseX, float mouseY, boolean mouseDown) {
		mouseX = (2f * mouseX) / framebufferWidth - 1f;
		mouseX /= (framebufferWidth != 0 ? framebufferHeight/framebufferWidth : 1);
		mouseY = -((2f * mouseY) / framebufferHeight - 1f);

		for(Entry<Element, DynamicLocation> entry : entrySet()) {
			if(updatePositions) {
				entry.getKey().setPositionX(getX(entry.getValue(), framebufferHeight/framebufferWidth));
				entry.getKey().setPositionY(getY(entry.getValue()));
			}
			
			if(entry.getKey() instanceof Button && entry.getValue() != null) {
				Button button = (Button)entry.getKey();
				boolean hovering = HitboxUtil.intersection(entry.getKey().getPositionX(), entry.getKey().getPositionY(), entry.getKey().getWidth(), entry.getKey().getHeight(), mouseX, mouseY);
				
				if(button.getHovered() && !hovering) button.onUnHover();
				else if(!button.getHovered() && hovering) button.onHover();
				if(hovering && mouseDown && !button.getPressed()) button.onPress();
				else if(!mouseDown && button.getPressed()) button.onRelease();
			}
		}
		updatePositions = false;
	}
	
	private static float getX(DynamicLocation location, float aspectRatio) {
		float position = location.horizontal/aspectRatio;
		if(position > 0) return position - location.paddingX;
		else if(position < 0) return position + location.paddingX;
		else return position;
	}
	
	private static float getY(DynamicLocation location) {
		if(location.vertical > 0) 
			return location.vertical - location.paddingY;
		else if(location.vertical < 0) 
			return location.vertical + location.paddingY;
		else 
			return location.vertical;
	}

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
