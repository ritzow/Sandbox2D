package ritzow.solomon.engine.world.entity;

import ritzow.solomon.engine.graphics.Graphics;
import ritzow.solomon.engine.graphics.Model;
import ritzow.solomon.engine.graphics.ModelRenderer;
import ritzow.solomon.engine.graphics.ModifiableGraphics;
import ritzow.solomon.engine.resource.Models;

public class TestEntity extends Entity {
	protected transient ModifiableGraphics graphics;
	protected float rotationSpeed;
	protected float width;
	protected float height;
	
	public TestEntity() {
		graphics = new ModifiableGraphics(Models.BLUE_SQUARE);
	}
	
	public TestEntity(Model model, float width, float height, float posX, float posY) {
		this.graphics = new ModifiableGraphics(model);
		this.width = width;
		this.height = height;
		this.positionX = posX;
		this.positionY = posY;
	}
	
	@SuppressWarnings("unused")
	private TestEntity(byte[] data) {
		throw new UnsupportedOperationException("not implemented");
	}
	
	public byte[] getBytes() {
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
