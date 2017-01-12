package ritzow.solomon.engine.world.entity;

import ritzow.solomon.engine.graphics.Graphics;
import ritzow.solomon.engine.graphics.ImmutableGraphics;
import ritzow.solomon.engine.graphics.Model;
import ritzow.solomon.engine.graphics.ModelRenderer;
import ritzow.solomon.engine.resource.Models;
import ritzow.solomon.engine.util.ByteUtil;

public class TestEntity extends Entity {
	protected Graphics graphics;
	protected float width;
	protected float height;
	
	public TestEntity() {
		this(Models.BLUE_SQUARE, 3, 3, 0, 0);
	}
	
	public TestEntity(Model model, float width, float height, float posX, float posY) {
		this.graphics = new ImmutableGraphics(model, 1.0f, width, height, 0.0f);
		this.width = width; 
		this.height = height;
		this.positionX = posX;
		this.positionY = posY;
	}
	
	public TestEntity(byte[] data) {
		super(data);
		width = ByteUtil.getFloat(data, 16);
		height = ByteUtil.getFloat(data, 20);
		graphics = new ImmutableGraphics(Models.BLUE_SQUARE, 1.0f, width, height, 0);
	}
	
	public byte[] getBytes() {
		byte[] superBytes = super.getBytes();
		byte[] bytes = new byte[superBytes.length + 8];
		ByteUtil.putFloat(bytes, superBytes.length, width);
		ByteUtil.putFloat(bytes, superBytes.length + 4, height);
		ByteUtil.copy(superBytes, bytes, 0);
		return bytes;
	}
	
	public Graphics getGraphics() {
		return graphics;
	}
	
	public void update(float time) {
		super.update(time);
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
		return width * height;
	}
}
