package graphics.ui.element;

import graphics.Renderable;
import graphics.Renderer;
import graphics.ui.DynamicLocation;
import graphics.ui.element.button.Button;
import input.handler.CursorPosHandler;
import input.handler.FramebufferSizeHandler;
import input.handler.MouseButtonHandler;
import java.util.HashMap;
import util.HitboxUtil;
import util.Updatable;

import static org.lwjgl.glfw.GLFW.*;

public final class ElementManager extends HashMap<Element, DynamicLocation> implements Renderable, Updatable, CursorPosHandler, MouseButtonHandler, FramebufferSizeHandler {
	private static final long serialVersionUID = 1509124666145321593L;
	
	protected volatile float cursorX;
	protected volatile float cursorY;
	protected volatile float frameWidth;
	protected volatile float frameHeight;
	protected volatile boolean mouseDown;
	
	@Override
	public void render(Renderer renderer) {
		renderer.loadViewMatrix(false);
		for(Entry<Element, DynamicLocation> entry : entrySet()) {
			if(entry.getValue() != null)
				entry.getKey().render(renderer, getX(entry.getValue(), frameHeight/frameWidth), getY(entry.getValue()));
		}
	}
	
	public void update() {
		update(frameWidth, frameHeight, cursorX, cursorY, mouseDown);
	}
	
	protected void update(float frameWidth, float frameHeight, float mouseX, float mouseY, boolean mouseDown) {
		mouseX = (2f * mouseX) / frameWidth - 1f;
		mouseX /= (frameWidth != 0 ? frameHeight/frameWidth : 1);
		mouseY = -((2f * mouseY) / frameHeight - 1f);

		for(Entry<Element, DynamicLocation> entry : entrySet()) {
			if(entry.getKey() instanceof Button && entry.getValue() != null) {
				Button button = (Button)entry.getKey();
				boolean onButton = HitboxUtil.intersection(
						getX(entry.getValue(), frameWidth != 0 ? frameHeight/frameWidth : 1), getY(entry.getValue()),
						button.getWidth(), button.getHeight(), mouseX, mouseY);
				
				if(button.getHovered() && !onButton) button.onUnHover();
				else if(!button.getHovered() && onButton) button.onHover();
				if(onButton && mouseDown && !button.getPressed()) button.onPress();
				else if(!mouseDown && button.getPressed()) button.onRelease();
			}
		}
	}
	
	private static float getX(DynamicLocation location, float aspectRatio) {
		float position = convertRange(location.horizontal, -1, 1, -1/aspectRatio, 1/aspectRatio);
		if(position > 0) return position - location.paddingX;
		else if(position < 0) return position + location.paddingX;
		else return position;
		
	}
	
	private static float getY(DynamicLocation location) {
		if(location.vertical > 0) return location.vertical - location.paddingY;
		else if(location.vertical < 0) return location.vertical + location.paddingY;
		else return location.vertical;
	}
	
	private static float convertRange(float value, float originalMin, float originalMax, float newMin, float newMax) {
		if(value > originalMax || value < originalMin) return 0;
		return (((value - originalMin) * (newMax - newMin)) / (originalMax - originalMin)) + newMin;
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
		frameWidth = width;
		frameHeight = height;
	}
}
