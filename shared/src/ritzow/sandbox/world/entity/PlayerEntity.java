package ritzow.sandbox.world.entity;

import ritzow.sandbox.data.ByteUtil;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.network.Protocol.PlayerAction;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.component.Inventory;
import ritzow.sandbox.world.component.Living;
import ritzow.sandbox.world.item.Item;

/**
 * Represents a player controlled by a human
 * @author Solomon Ritzow
 */
public class PlayerEntity extends Entity implements Living {
	protected final Inventory<Item> inventory;
	protected int selected, health;
	protected boolean left, right, up, down;
	
	public PlayerEntity(int entityID) {
		super(entityID);
		this.inventory = new Inventory<>(9);
	}
	
	public PlayerEntity(DataReader input) {
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
		ByteUtil.copy(superBytes, bytes, 0);
		ByteUtil.copy(invBytes, bytes, superBytes.length);
		ByteUtil.putInteger(bytes, superBytes.length + invBytes.length, health);
		ByteUtil.putInteger(bytes, superBytes.length + invBytes.length + 4, selected);
		return bytes;
	}
	
	@Override
	public void update(World world, float time) {
		super.update(world, time);
		if(left)
			velocityX = -getMass();
		else if(right)
			velocityX = getMass();
		if(up && blockBelow(world.getForeground()))
			velocityY = getMass();
	}
	
	private boolean blockBelow(BlockGrid blocks) {
		return blockInRectangle(blocks, 
				positionX - getWidth()/2 + 0.1f, positionY - getHeight()/2 - 0.1f, 
				positionX + getWidth()/2 - 0.1f, positionY - getHeight()/2);
	}
	
	private static boolean blockInRectangle(BlockGrid blocks, float x1, float y1, float x2, float y2) {
		int a1 = Math.round(x1), b1 = Math.round(y1), a2 = Math.round(x2), b2 = Math.round(y2);
		for(int x = a1; x <= a2; x++) {
			for(int y = b1; y <= b2; y++) {
				if(blocks.isBlock(x, y))
					return true;
			}
		}
		return false;
	}
	
	public void processAction(PlayerAction action, boolean enabled) {
		switch(action) {
			case MOVE_LEFT:
				left = enabled; break;
			case MOVE_RIGHT:
				right = enabled; break;
			case MOVE_UP:
				up = enabled; break;
			case MOVE_DOWN:
				down = enabled; break;
		}
	}
	
	public Inventory<Item> getInventory() {
		return inventory;
	}
	
	public Item removeSelectedItem() {
		return inventory.remove(selected);
	}
	
	public Item getSelectedItem() {
		return inventory.get(selected);
	}
	
	public void setSlot(int slot) {
		selected = Math.min(Math.max(slot, 0), inventory.getSize());
	}

	@Override
	public boolean getShouldDelete() {
		return false;
	}

	@Override
	public boolean doCollision() {
		return true;
	}

	@Override
	public boolean doBlockCollisionResolution() {
		return true;
	}

	@Override
	public boolean doEntityCollisionResolution() {
		return true;
	}

	@Override
	public float getFriction() {
		return 0.02f;
	}

	@Override
	public float getWidth() {
		return 1;
	}

	@Override
	public float getHeight() {
		return 2;
	}

	@Override
	public float getMass() {
		return 0.2f;
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
