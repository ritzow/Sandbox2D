package ritzow.solomon.engine.world.entity;

import ritzow.solomon.engine.graphics.Graphics;
import ritzow.solomon.engine.graphics.ModelRenderer;

public class ParticleEntity extends Entity {
	protected final Graphics graphics;
	protected float rotation;
	protected float opacity;
	protected float rotationSpeed;
	protected boolean fade;
	protected boolean shouldDelete;
	protected long birthtime;
	protected long lifetime;

	public ParticleEntity(Graphics graphics, float posX, float posY, float velocityX, float velocityY,  float rotationSpeed, long lifetime, boolean fade) {
		this.graphics = graphics;
		this.positionX = posX;
		this.positionY = posY;
		this.velocityX = velocityX;
		this.velocityY = velocityY;
		this.rotationSpeed = rotationSpeed;
		this.lifetime = lifetime;
		this.fade = fade;
		this.birthtime = System.currentTimeMillis();
	}
	
	public ParticleEntity(byte[] data) {
		throw new UnsupportedOperationException("not implemented");
	}
	
	@Override
	public byte[] getBytes() {
		throw new UnsupportedOperationException("not implemented");
	}
	
	public void update(float time) {
		float remaining = getLifetimeRemaining();
		
		if(remaining <= 0) {
			this.shouldDelete = true;
		}
		
		else {
			super.update(time);
			rotation = rotation + rotationSpeed * time;
			
			if(fade) {
				opacity = remaining/lifetime;
			}
		}
	}

	@Override
	public void render(ModelRenderer renderer) {
		graphics.render(renderer, positionX, positionY, opacity, 1, 1, rotation);
	}
	
	protected long getLifetimeRemaining() {
		return lifetime - (System.currentTimeMillis() - birthtime);
	}

	@Override
	public boolean getShouldDelete() {
		return shouldDelete;
	}

	@Override
	public boolean doCollision() {
		return false;
	}

	@Override
	public boolean doBlockCollisionResolution() {
		return false;
	}

	@Override
	public boolean doEntityCollisionResolution() {
		return false;
	}

	@Override
	public float getFriction() {
		return 0;
	}

	@Override
	public float getWidth() {
		return graphics.getScaleX();
	}

	@Override
	public float getHeight() {
		return graphics.getScaleY();
	}

	@Override
	public float getMass() {
		return 0;
	}
}
