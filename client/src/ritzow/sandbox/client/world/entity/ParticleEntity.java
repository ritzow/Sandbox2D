package ritzow.sandbox.client.world.entity;

import ritzow.sandbox.client.graphics.ModelRenderProgram;
import ritzow.sandbox.client.graphics.Renderable;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;

public class ParticleEntity extends Entity implements Renderable {
	private final int model;
	private final float scaleX, scaleY, rotationSpeed;
	private float rotation, opacity;
	private final long birthtime, lifetime;
	private final boolean fade;
	private boolean remove;

	public ParticleEntity(int entityID, int model, float scaleX, float scaleY, long lifetime, boolean fade) {
		super(entityID);
		this.model = model;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		this.fade = fade;
		this.lifetime = lifetime;
		this.rotationSpeed = Utility.randomFloat(-0.5f, 0.5f);
		this.birthtime = System.currentTimeMillis();
	}
	
	@Override
	public void update(World world, float time) {
		float remaining = getLifetimeRemaining();
		
		if(remaining <= 0) {
			this.remove = true;
		}
		
		else {
			super.update(world, time);
			rotation = rotation + rotationSpeed * time;
			
			if(fade) {
				opacity = remaining/lifetime;
			}
		}
	}

	@Override
	public void render(ModelRenderProgram renderer) {
		renderer.render(model, opacity, positionX, positionY, scaleX, scaleY, rotation);
	}
	
	protected long getLifetimeRemaining() {
		return lifetime - (System.currentTimeMillis() - birthtime);
	}

	@Override
	public boolean getShouldDelete() {
		return remove;
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
		return scaleX;
	}

	@Override
	public float getHeight() {
		return scaleY;
	}

	@Override
	public float getMass() {
		return 0;
	}
}
