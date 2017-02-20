package ritzow.solomon.engine.world.entity;

import ritzow.solomon.engine.graphics.ModelRenderer;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.util.Transportable;
import ritzow.solomon.engine.world.base.World;

public abstract class Entity implements Transportable {
	protected final int entityID;
	
	protected float 
		positionX,
		positionY,
		velocityX,
		velocityY;
	
	public void update(float time) {
		positionX += velocityX * time;
		positionY += velocityY * time;
	}
	
	public Entity(int entityID) {
		this.entityID = entityID;
	}
	
	public Entity(byte[] data) {
		entityID = ByteUtil.getInteger(data, 0);
		positionX = ByteUtil.getFloat(data, 4);
		positionY = ByteUtil.getFloat(data, 8);
		velocityX = ByteUtil.getFloat(data, 12);
		velocityY = ByteUtil.getFloat(data, 16);
	}
	
	public byte[] getBytes() {
		byte[] data = new byte[20];
		ByteUtil.putInteger(data, 0, entityID);
		ByteUtil.putFloat(data, 4, positionX);
		ByteUtil.putFloat(data, 8, positionY);
		ByteUtil.putFloat(data, 12, velocityX);
		ByteUtil.putFloat(data, 16, velocityY);
		return data;
	}
	
	public final int getID() {
		 return entityID;
	}
	
	public void render(ModelRenderer renderer) {/* optional implementation */}
	public void onCollision(World world, Entity e, float time) {/* optional implementation */}

	/** @return true if the entity should be removed from the world **/
	public abstract boolean getShouldDelete();
	
	/** @return true if the entity's onCollision method should be called when colliding with another entity **/
	public abstract boolean doCollision();
	
	/** @return true if the entity should collide with solid blocks rather than fall through them **/
	public abstract boolean doBlockCollisionResolution();
	
	/** @return true if the entity should collide with other entities rather than passing through them **/
	public abstract boolean doEntityCollisionResolution();
	
	/** @return the roughness of the surface of the entity **/
	public abstract float getFriction();
	
	/** @return the physical width of the entity used in collision processing **/
	public abstract float getWidth();
	
	/** @return the physical height of the entity used in collision processing **/
	public abstract float getHeight();
	
	/** @return the mass of the entity **/
	public abstract float getMass();
	
	/** @return the speed of the Entity based on the velocity in the x and y directions **/
	public final float getSpeed() {
		return (float)Math.abs(Math.sqrt(velocityX * velocityX + velocityY * velocityY));
	}

	/** @return the horizontal position of the of the entity in the world **/
	public final float getPositionX() {
		return positionX;
	}
	
	/** @return the vertical position of the of the entity in the world **/
	public final float getPositionY() {
		return positionY;
	}

	/** @return the distance the entity should move in the horizontal direction each game update **/
	public final float getVelocityX() {
		return velocityX;
	}

	/** @return the distance the entity should move in the vertical direction each game update **/
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