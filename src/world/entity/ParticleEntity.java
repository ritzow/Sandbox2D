package world.entity;

import graphics.Renderer;
import world.World;
import world.entity.component.Graphics;

public class ParticleEntity extends Entity {
	private static final long serialVersionUID = 7392722072442474321L;
	
	protected Graphics graphics;
	protected long birthtime;
	protected long lifetime;
	protected float rotationSpeed;
	protected boolean fade;

	public ParticleEntity(Graphics graphics, float posX, float posY, float velocityX, float velocityY, long lifetime, float rotationSpeed, boolean fade) {
		this.graphics = graphics;
		this.positionX = posX;
		this.positionY = posY;
		this.velocityX = velocityX;
		this.velocityY = velocityY;
		this.rotationSpeed = rotationSpeed;
		this.lifetime = lifetime;
		this.fade = fade;
		this.doCollision = false;
		this.doBlockCollisionResolution = false;
		this.doEntityCollisionResolution = false;
		this.birthtime = System.currentTimeMillis();
	}
	
	public void startLife() {
		
	}
	
	public void update(float milliseconds) {
		float remaining = this.getLifetimeRemaining();
		
		if(remaining <= 0) {
			this.shouldDelete = true;
		}
		
		else {
			super.update(milliseconds);
			graphics.setRotation(graphics.getRotation() + rotationSpeed);
			
			if(fade) {
				float opacity = remaining/lifetime;
				graphics.setOpacity(opacity);
			}
		}
	}

	@Override
	public void render(Renderer renderer) {
		graphics.render(renderer, positionX, positionY);
	}

	@Override
	public void onCollision(World world, Entity e) {
		
	}
	
	protected float getLifetimeRemaining() {
		return lifetime - (System.currentTimeMillis() - birthtime);
	}
}
