package graphics.ui.element.button;

import graphics.ui.element.GraphicsElement;
import util.ModelManager;

public class TestButton extends GraphicsElement implements Button {
	protected boolean hovered;
	protected boolean pressed;
	
	{
		graphics.scale().setX(0.25f);
		graphics.scale().setY(0.25f);
		
	}
	
	public TestButton() {
		super(ModelManager.BLUE_SQUARE, 0, 0);
	}
	
	@Override
	public void onPress() {
		pressed = true;
		graphics.setModel(ModelManager.RED_SQUARE);
	}

	@Override
	public void onRelease() {
		pressed = false;
		graphics.setModel(ModelManager.BLUE_SQUARE);
	}

	@Override
	public void onHover() {
		hovered = true;
		this.graphics.scale().setX(this.graphics.scale().getX() + 0.05f);
		this.graphics.scale().setY(this.graphics.scale().getY() + 0.05f);
	}

	@Override
	public void onUnHover() {
		hovered = false;
		graphics.scale().setX(this.graphics.scale().getX() - 0.05f);
		graphics.scale().setY(this.graphics.scale().getY() - 0.05f);
	}

	@Override
	public float getWidth() {
		return graphics.scale().getX();
	}

	@Override
	public float getHeight() {
		return graphics.scale().getY();
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
