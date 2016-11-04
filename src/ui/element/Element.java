package ui.element;

import graphics.ModelRenderer;

public abstract class Element {
	
	protected float positionX;
	protected float positionY;
	
	public abstract void render(ModelRenderer renderer, float x, float y);
	
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
