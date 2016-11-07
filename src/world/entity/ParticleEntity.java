package world.entity;

import graphics.ModelRenderer;
import world.World;
import world.entity.component.Graphics;

public class ParticleEntity extends Entity {
	private static final long serialVersionUID = 7392722072442474321L;
	
	protected Graphics graphics;
	protected final long birthtime;
	protected long lifetime;
	protected float rotationSpeed;
	protected boolean fade;
	
	protected boolean shouldDelete;

	public ParticleEntity(Graphics graphics, float posX, float posY, float velocityX, float velocityY, long lifetime, float rotationSpeed, boolean fade) {
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
	public void render(ModelRenderer renderer) {
		graphics.render(renderer, positionX, positionY);
	}

	@Override
	public void onCollision(World world, Entity e, float time) {
		
	}
	
	protected float getLifetimeRemaining() {
		return lifetime - (System.currentTimeMillis() - birthtime);
	}

	@Override
	public boolean getShouldDelete() {
		return shouldDelete;
	}

	@Override
	public boolean getDoCollision() {
		return false;
	}

	@Override
	public boolean getDoBlockCollisionResolution() {
		return false;
	}

	@Override
	public boolean getDoEntityCollisionResolution() {
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
