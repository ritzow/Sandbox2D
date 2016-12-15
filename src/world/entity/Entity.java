package world.entity;

import graphics.ModelRenderer;
import util.ByteUtil;
import util.Transportable;
import world.World;

public abstract class Entity implements Transportable {
	protected float positionX;
	protected float positionY;
	protected float velocityX;
	protected float velocityY;
	
	public void update(float time) {
		positionX += velocityX * time;
		positionY += velocityY * time;
	}
	
	public Entity() {
		
	}
	
	public Entity(byte[] data) {
		positionX = ByteUtil.getFloat(data, 0);
		positionY = ByteUtil.getFloat(data, 4);
		velocityX = ByteUtil.getFloat(data, 8);
		velocityY = ByteUtil.getFloat(data, 12);
	}
	
	public byte[] toBytes() {
		byte[] data = new byte[16];
		ByteUtil.putFloat(data, 0, positionX);
		ByteUtil.putFloat(data, 4, positionY);
		ByteUtil.putFloat(data, 8, velocityX);
		ByteUtil.putFloat(data, 12, velocityY);
		return data;
	}
	
	public void render(ModelRenderer renderer) {/* optional implementation */}
	public void onCollision(World world, Entity e, float time) {/* optional implementation */}

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