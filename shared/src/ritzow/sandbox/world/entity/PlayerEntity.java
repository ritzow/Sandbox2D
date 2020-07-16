package ritzow.sandbox.world.entity;

import java.util.Objects;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.component.Inventory;
import ritzow.sandbox.world.component.Living;
import ritzow.sandbox.world.item.Item;

/**
 * Represents a player controlled by a human
 * TODO implement dead reckoning for client and server
 * @author Solomon Ritzow
 */
public abstract class PlayerEntity extends Entity implements Living {
	protected final Inventory<Item> inventory;
	protected byte selected;
	protected int health;
	protected boolean left, right, up, down, isGrounded;

	protected static final boolean JETPACK_MODE = false;
	protected static final float
			SIZE_SCALE		= 1f,
			MOVEMENT_SPEED	= Utility.convertPerSecondToPerNano(
				12.5f /* human running speed meters */) * SIZE_SCALE,
			JUMP_VELOCITY	= Utility.convertPerSecondToPerNano(11),
			AIR_ACCELERATION	= Utility.convertAccelerationSecondsNanos(50),
			AIR_MAX_SPEED = 0.5f * MOVEMENT_SPEED;

	public PlayerEntity(int entityID) {
		super(entityID);
		this.inventory = new Inventory<>(3);
	}

	public PlayerEntity(TransportableDataReader input) {
		super(input);
		inventory = input.readObject();
		health = input.readInteger();
		selected = input.readByte();
	}

	@Override
	public byte[] getBytes(Serializer ser) {
		byte[] superBytes = super.getBytes(ser);
		byte[] invBytes = ser.serialize(inventory);
		byte[] bytes = new byte[superBytes.length + invBytes.length + 4 + 1];
		Bytes.copy(superBytes, bytes, 0);
		Bytes.copy(invBytes, bytes, superBytes.length);
		Bytes.putInteger(bytes, superBytes.length + invBytes.length, health);
		bytes[superBytes.length + invBytes.length + 4] = selected;
		return bytes;
	}

	@SuppressWarnings("unused")
	@Override
	public void update(World world, long ns) {
		if(isGrounded) {
			if(left && right) {
				velocityX = 0;
			} else if(left) {
				velocityX = -MOVEMENT_SPEED;
			} else if(right) {
				velocityX = MOVEMENT_SPEED;
			}
			if(!JETPACK_MODE && up) {
				velocityY = JUMP_VELOCITY;
			}
		} else if(left && !right) {
			velocityX = Math.max(Math.min(-AIR_MAX_SPEED, velocityX), Math.fma(-AIR_ACCELERATION, ns, velocityX));
		} else if(right && !left) {
			velocityX = Math.min(Math.max(AIR_MAX_SPEED, velocityX), Math.fma(AIR_ACCELERATION, ns, velocityX));
		}

		if(JETPACK_MODE && up) {
			velocityY = Math.fma(AIR_ACCELERATION, ns, velocityY);
		}

		isGrounded = false; //in case there might not be blocks below during next update
		super.update(world, ns);
	}

	@Override
	public void onCollision(World world, Block block, Side side, int blockX, int blockY, long ns) {
		isGrounded = isGrounded || side == Side.BOTTOM;
	}

	public void setLeft(boolean left) {
		this.left = left;
	}

	public void setRight(boolean right) {
		this.right = right;
	}

	public void setUp(boolean up) {
		this.up = up;
	}

	public void setDown(boolean down) {
		if(this.down ^ down) positionY = Math.fma(SIZE_SCALE, down ? -0.5f : 0.5f, positionY);
		this.down = down;
	}

	public boolean isLeft() {
		return left;
	}

	public boolean isRight() {
		return right;
	}

	public boolean isUp() {
		return up;
	}

	public boolean isDown() {
		return down;
	}

	public byte selected() {
		return selected;
	}

	public Inventory<Item> inventory() {
		return inventory;
	}

	public Item removeSelected() {
		return inventory.remove(selected);
	}

	public Item getSelected() {
		return inventory.get(selected);
	}

	public void setSlot(int slot) {
		selected = (byte)Objects.checkIndex(slot, inventory.getSize());
	}

	@Override
	public int getHealth() {
		return health;
	}

	@Override
	public int getMaxHealth() {
		return 100;
	}

	@Override
	public void setHealth(int health) {
		if(health > getMaxHealth() || health < 0)
			throw new IllegalArgumentException("invalid health amount");
		this.health = health;
	}

	@Override
	public boolean getShouldDelete() {
		return false;
	}

	@Override
	public boolean interactsWithEntities() {
		return false;
	}

	@Override
	public boolean collidesWithBlocks() {
		return true;
	}

	@Override
	public boolean collidesWithEntities() {
		return false;
	}

	@Override
	public float getFriction() {
		return (down ? 0.01f : 0.04f)/16;
	}

	@Override
	public float getWidth() {
		return SIZE_SCALE;
	}

	@Override
	public float getHeight() {
		return SIZE_SCALE * (down ? 1 : 2);
	}

	@Override
	public float getMass() {
		return 0.2f * SIZE_SCALE;
	}

	@Override
	public int hashCode() {
		return entityID;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof PlayerEntity p && p.entityID == entityID;
	}
}
