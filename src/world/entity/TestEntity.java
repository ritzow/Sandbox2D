package world.entity;

import graphics.Model;
import graphics.ModelRenderer;
import resource.Models;
import world.entity.component.Graphics;

public class TestEntity extends Entity {
	protected transient Graphics graphics;
	protected float rotationSpeed;
	protected float width;
	protected float height;
	
	public TestEntity() {
		graphics = new Graphics(Models.BLUE_SQUARE);
	}
	
	public TestEntity(Model model, float width, float height, float posX, float posY) {
		this.graphics = new Graphics(model);
		this.width = width;
		this.height = height;
		this.positionX = posX;
		this.positionY = posY;
	}
	
	@SuppressWarnings("unused")
	private TestEntity(byte[] data) {
		throw new UnsupportedOperationException("not implemented");
	}
	
	public byte[] toBytes() {
		throw new UnsupportedOperationException("not implemented");
	}
	
	public Graphics getGraphics() {
		return graphics;
	}
	
	public void update(float time) {
		super.update(time);
		graphics.setRotation(graphics.getRotation() + rotationSpeed * time);
	}
	
	public void render(ModelRenderer renderer) {
		graphics.render(renderer, positionX, positionY);
	}

	@Override
	public boolean getShouldDelete() {
		return false;
	}

	@Override
	public boolean doCollision() {
		return true;
	}

	@Override
	public boolean doBlockCollisionResolution() {
		return true;
	}

	@Override
	public boolean doEntityCollisionResolution() {
		return true;
	}

	@Override
	public float getFriction() {
		return 0.05f;
	}

	@Override
	public float getWidth() {
		return width;
	}

	@Override
	public float getHeight() {
		return height;
	}

	@Override
	public float getMass() {
		return 10;
	}
}
