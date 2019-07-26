package ritzow.sandbox.world.entity;

import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.data.Transportable;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;

public abstract class Entity implements Transportable {
	protected final int entityID;
	
	protected float 
		positionX,
		positionY,
		velocityX,
		velocityY;
	
	public void update(World world, long nanoseconds) {
		positionX = Math.fma(velocityX, nanoseconds, positionX);
		positionY = Math.fma(velocityY, nanoseconds, positionY);
	}
	
	public Entity(int entityID) {
		this.entityID = entityID;
	}
	
	public Entity(DataReader reader) {
		entityID = reader.readInteger();
		positionX = reader.readFloat();
		positionY = reader.readFloat();
		velocityX = reader.readFloat();
		velocityY = reader.readFloat();
	}
	
	@Override
	public byte[] getBytes(Serializer ser) {
		byte[] data = new byte[20];
		Bytes.putInteger(data, 0, entityID);
		Bytes.putFloat(data, 4, positionX);
		Bytes.putFloat(data, 8, positionY);
		Bytes.putFloat(data, 12, velocityX);
		Bytes.putFloat(data, 16, velocityY);
		return data;
	}
	
	public final int getID() {
		 return entityID;
	}
	
	public static enum Side {
		TOP,
		BOTTOM,
		LEFT,
		RIGHT;
	}
	
	public void onCollision(World world, Entity e, long ns) {/* optional implementation */}
	public void onCollision(World world, Block block, Side side, int blockX, int blockY, long ns) {/* optional implementation */}

	/** @return true if the entity should be removed from the world **/
	public abstract boolean getShouldDelete();
	
	/** @return true if the entity should collide with solid blocks rather than fall through them **/
	public abstract boolean collidesWithBlocks();
	
	/** @return true if the entity should collide with other entities rather than passing through them **/
	public abstract boolean collidesWithEntities();
	
	/** @return true if the entity has physics or custom collision logic with other entities **/
	public abstract boolean interactsWithEntities();
	
	/** @return the roughness of the surface of the entity **/
	public abstract float getFriction();
	
	/** @return the physical width of the entity used in collision processing **/
	public abstract float getWidth();
	
	/** @return the physical height of the entity used in collision processing **/
	public abstract float getHeight();
	
	/** @return the mass of the entity **/
	public abstract float getMass();

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
	
	@Override
	public String toString() {
		return new StringBuilder()
			.append("positionX = ")
			.append(positionX)
			.append(", positionY = ")
			.append(positionY)
			.append(", velocityX = ")
			.append(velocityX)
			.append(", velocityY = ")
			.append(velocityY).toString();
	}
}