package ui.element.button;

import resource.Models;
import ui.element.GraphicsElement;

public class TestButton extends GraphicsElement implements Button {
	protected boolean hovered;
	protected boolean pressed;
	
	{
		graphics.getScale().setX(0.25f);
		graphics.getScale().setY(0.25f);
		
	}
	
	public TestButton() {
		super(Models.BLUE_SQUARE, 0, 0);
	}
	
	@Override
	public void onPress() {
		pressed = true;
		graphics.setModel(Models.RED_SQUARE);
	}

	@Override
	public void onRelease() {
		pressed = false;
		graphics.setModel(Models.BLUE_SQUARE);
	}

	@Override
	public void onHover() {
		hovered = true;
		this.graphics.getScale().setX(this.graphics.getScale().getX() + 0.05f);
		this.graphics.getScale().setY(this.graphics.getScale().getY() + 0.05f);
	}

	@Override
	public void onUnHover() {
		hovered = false;
		graphics.getScale().setX(this.graphics.getScale().getX() - 0.05f);
		graphics.getScale().setY(this.graphics.getScale().getY() - 0.05f);
	}

	@Override
	public float getWidth() {
		return graphics.getScale().getX();
	}

	@Override
	public float getHeight() {
		return graphics.getScale().getY();
	}
	
	@Override
	public boolean getHovered() {
		return hovered;
	}

	@Override
	public boolean getPressed() {
		return pressed;
	}

}
