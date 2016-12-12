package world.entity;

import graphics.ModelRenderer;
import world.World;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public abstract class Entity implements Externalizable {
	private static final long serialVersionUID = 7177412000462430179L;
	
	protected float positionX;
	protected float positionY;
	protected float velocityX;
	protected float velocityY;
	
	public void update(float time) {
		positionX += velocityX * time;
		positionY += velocityY * time;
	}
	
	public abstract void render(ModelRenderer renderer);
	
	public void onCollision(World world, Entity e, float time) {
		
	}
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeFloat(positionX);
		out.writeFloat(positionY);
		out.writeFloat(velocityX);
		out.writeFloat(velocityY);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		positionX = in.readFloat();
		positionY = in.readFloat();
		velocityX = in.readFloat();
		velocityY = in.readFloat();
	}

	public abstract boolean getShouldDelete();
	public abstract boolean doCollision();
	public abstract boolean doBlockCollisionResolution();
	public abstract boolean doEntityCollisionResolution();
	public abstract float getFriction();
	public abstract float getWidth();
	public abstract float getHeight();
	public abstract float getMass();
	
	public final float getSpeed() {
		return (float)Math.abs(Math.sqrt(velocityX * velocityX + velocityY * velocityY));
	}

	public final float getPositionX() {
		return positionX;
	}

	public final float getPositionY() {
		return positionY;
	}

	public final float getVelocityX() {
		return velocityX;
	}

	public final float getVelocityY() {
		return velocityY;
	}

	public final void setPositionX(float positionX) {
		this.positionX = positionX;
	}

	public final void setPositionY(float positionY) {
		this.positionY = positionY;
	}

	public final void setVelocityX(float velocityX) {
		this.velocityX = velocityX;
	}

	public final void setVelocityY(float velocityY) {
		this.velocityY = velocityY;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("positionX = ");
		builder.append(positionX);
		builder.append(", positionY = ");
		builder.append(positionY);
		builder.append(", velocityX = ");
		builder.append(velocityX);
		builder.append(", velocityY = ");
		builder.append(velocityY);
		return builder.toString();
	}
}