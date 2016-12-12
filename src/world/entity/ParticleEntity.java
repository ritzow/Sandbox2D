package world.entity;

import graphics.ModelRenderer;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import world.entity.component.Graphics;

public class ParticleEntity extends Entity {
	private static final long serialVersionUID = 7392722072442474321L;
	
	protected final Graphics graphics;
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
	
	public void update(float time) {
		float remaining = getLifetimeRemaining();
		
		if(remaining <= 0) {
			this.shouldDelete = true;
		}
		
		else {
			super.update(time);
			graphics.setRotation(graphics.getRotation() + rotationSpeed * time);
			
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

	/*
	protected final Graphics graphics;
	protected float rotationSpeed;
	protected final boolean fade;
	protected boolean shouldDelete;
	protected final long birthtime;
	protected final long lifetime;
	 */
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeFloat(rotationSpeed);
		out.writeBoolean(fade);
		out.writeBoolean(shouldDelete);
		out.writeLong(birthtime);
		out.writeLong(lifetime);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		rotationSpeed = in.readFloat();
		fade = in.readBoolean();
		shouldDelete = in.readBoolean();
		birthtime = in.readLong();
		lifetime = in.readLong();
	}
	
	
}
