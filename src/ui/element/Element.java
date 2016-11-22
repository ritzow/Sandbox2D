package ui.element;

import graphics.Renderable;

public abstract class Element implements Renderable {
	
	protected float positionX;
	protected float positionY;
	
	public abstract float getWidth();
	public abstract float getHeight();

	public float getPositionX() {
		return positionX;
	}

	public float getPositionY() {
		return positionY;
	}

	public void setPositionX(float positionX) {
		this.positionX = positionX;
	}

	public void setPositionY(float positionY) {
		this.positionY = positionY;
	}
}
