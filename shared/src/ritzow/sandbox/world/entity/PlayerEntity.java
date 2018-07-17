package ritzow.sandbox.world.entity;

import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.network.Protocol.PlayerAction;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.component.Inventory;
import ritzow.sandbox.world.component.Living;
import ritzow.sandbox.world.item.Item;

/**
 * Represents a player controlled by a human
 * @author Solomon Ritzow
 */
public abstract class PlayerEntity extends Entity implements Living {
	protected final Inventory<Item> inventory;
	protected int selected, health;
	protected boolean left, right, up, down;
	
	protected static boolean JETPACK_MODE = false;
	protected static final float 
			SIZE_SCALE		= 1f,
			MOVEMENT_SPEED	= 0.2f * SIZE_SCALE,
			JUMP_VELOCITY	= MOVEMENT_SPEED * 1.5f,
			AIR_MOVEMENT	= MOVEMENT_SPEED / 7.5f;
	
	public PlayerEntity(int entityID) {
		super(entityID);
		this.inventory = new Inventory<>(9);
	}
	
	public PlayerEntity(TransportableDataReader input) {
		super(input);
		inventory = input.readObject();
		health = input.readInteger();
		selected = input.readInteger();
	}
	
	@Override
	public byte[] getBytes(Serializer ser) {
		byte[] superBytes = super.getBytes(ser);
		byte[] invBytes = ser.serialize(inventory);
		byte[] bytes = new byte[superBytes.length + invBytes.length + 4 + 4];
		Bytes.copy(superBytes, bytes, 0);
		Bytes.copy(invBytes, bytes, superBytes.length);
		Bytes.putInteger(bytes, superBytes.length + invBytes.length, health);
		Bytes.putInteger(bytes, superBytes.length + invBytes.length + 4, selected);
		return bytes;
	}
	
	@Override
	public void update(World world, float time) {
		if(blockBelow(world.getForeground())) {
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
			velocityX = Math.max(-MOVEMENT_SPEED, velocityX - time * AIR_MOVEMENT);
		} else if(right && !left) {
			velocityX = Math.min(MOVEMENT_SPEED, velocityX + time * AIR_MOVEMENT);
		}
		
		if(JETPACK_MODE && up) {
			velocityY += AIR_MOVEMENT * time;
		}
		
		super.update(world, time);
	}
	
	private boolean blockBelow(BlockGrid blocks) {
		return blockInRectangle(blocks, 
				positionX - getWidth()/2 + 0.1f, 	//x1
				positionY - getHeight()/2 - 0.1f, 	//y1
				positionX + getWidth()/2 - 0.1f, 	//x2
				positionY - getHeight()/2); 		//y2
	}
	
	private static boolean blockInRectangle(BlockGrid blocks, float x1, float y1, float x2, float y2) {
		int a1 = Utility.clampLowerBound(0, Math.round(x1)), 
			b1 = Utility.clampLowerBound(0, Math.round(y1)), 
			a2 = Utility.clampUpperBound(blocks.getWidth()-1, Math.round(x2)), 
			b2 = Utility.clampUpperBound(blocks.getHeight()-1, Math.round(y2));
		for(int x = a1; x <= a2; x++) {
			for(int y = b1; y <= b2; y++) {
				if(blocks.isBlock(x, y) && blocks.get(x, y).isSolid())
					return true;
			}
		}
		return false;
	}

	public void processAction(PlayerAction action, boolean isEnabled) {
		switch(action) {
			case MOVE_LEFT:
				left = isEnabled; break;
			case MOVE_RIGHT:
				right = isEnabled; break;
			case MOVE_UP:
				up = isEnabled; break;
			case MOVE_DOWN:
				onCrouch(down = isEnabled);
				break;
		}
	}
	
	protected void onCrouch(boolean isCrouched) {
		positionY += isCrouched ? -SIZE_SCALE/2 : SIZE_SCALE/2;
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
		if(slot > inventory.getSize() - 1 || slot < 0)
			throw new IllegalArgumentException("slot out of bounds");
		selected = slot;
	}

	@Override
	public boolean getShouldDelete() {
		return false;
	}

	@Override
	public boolean hasEntityCollisionLogic() {
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
		return (down ? 0.0075f : 0.02f);
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
}
