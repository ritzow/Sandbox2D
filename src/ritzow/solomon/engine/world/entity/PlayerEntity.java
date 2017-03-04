package ritzow.solomon.engine.world.entity;

import ritzow.solomon.engine.audio.Sounds;
import ritzow.solomon.engine.graphics.ModelRenderer;
import ritzow.solomon.engine.graphics.Models;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.world.base.World;
import ritzow.solomon.engine.world.component.Inventory;
import ritzow.solomon.engine.world.component.Living;
import ritzow.solomon.engine.world.item.Item;

/**
 * Represents a player controlled by a human
 * @author Solomon Ritzow
 */
public class PlayerEntity extends Entity implements Living {
	protected final Inventory inventory;
	protected volatile int selected;
	protected volatile int health;
	protected volatile boolean left;
	protected volatile boolean right;
	protected volatile boolean up;
	protected volatile boolean down;
	
	public PlayerEntity(int entityID) {
		super(entityID);
		this.inventory = new Inventory(9);
	}
	
	public PlayerEntity(byte[] data) throws ReflectiveOperationException {
		super(data);
		this.inventory = (Inventory)ByteUtil.deserialize(data, 20);
		this.health = ByteUtil.getInteger(data, 20 + ByteUtil.getSerializedLength(data, 20));
		this.selected = ByteUtil.getInteger(data, 20 + ByteUtil.getSerializedLength(data, 20) + 4);
		selected = 0;
	}
	
	@Override
	public byte[] getBytes() {
		byte[] superBytes = super.getBytes();
		byte[] invBytes = ByteUtil.serialize(inventory);
		byte[] bytes = new byte[superBytes.length + invBytes.length + 4 + 4];
		ByteUtil.copy(superBytes, bytes, 0);
		ByteUtil.copy(invBytes, bytes, superBytes.length);
		ByteUtil.putInteger(bytes, superBytes.length + invBytes.length, health);
		ByteUtil.putInteger(bytes, superBytes.length + invBytes.length + 4, selected);
		return bytes;
	}
	
	@Override
	public void update(float time) {
		if(left)
			velocityX = -getMass();
		else if(right)
			velocityX = getMass();
		if(up) {
			velocityY = getMass();
			up = false;
		}
		positionX += velocityX * time;
		positionY += velocityY * time;
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
		this.down = down;
	}

	@Override
	public void render(ModelRenderer renderer) {
		renderer.renderModel(Models.GREEN_FACE_INDEX, 1.0f, positionX, positionY + 0.5f, 1.0f, 1.0f, 0.0f);
		renderer.renderModel(Models.RED_SQUARE_INDEX, 1.0f, positionX, positionY - 0.5f, 1.0f, 1.0f, positionX);
		Item selectedItem = inventory.get(selected);
		if(selectedItem != null) {
			renderer.renderGraphics(selectedItem.getGraphics(), positionX + velocityX * 2, positionY, 
					1.0f, 0.5f, 0.5f, velocityX != 0 ? (float)Math.PI/4 * (velocityX < 0 ? -1 : 1) : 0);
		}
	}

	@Override
	public void onCollision(World world, Entity e, float time) {
		if(e instanceof ItemEntity) {
			if(inventory.add(((ItemEntity)e).getItem())) {
				world.remove(e);
				world.getAudioSystem().playSound(Sounds.ITEM_PICKUP, e.getPositionX(), e.getPositionY(), 0, 0.2f, 1, (float)Math.random() * 0.4f + 0.8f);
			}
		}
	}
	
	protected Item dropItem(World world, int slot) { //TODO should this be implemented here, or should I just use removeSelectedItem?
		Item item = inventory.remove(slot);
		if(item != null) {
			ItemEntity entity = new ItemEntity(0, item, positionX, positionY + this.getHeight()); //TODO deal with entityID for entities created outside server directly
			entity.setVelocityX((float) (Math.random() * 0.4f - 0.2f));
			entity.setVelocityY((float) (Math.random() * 0.3f));
			world.add(entity);
			selected = (selected == inventory.getSize() - 1 ? selected = 0 : selected + 1);
			world.getAudioSystem().playSound(Sounds.THROW, positionX, positionY, entity.getVelocityX(), entity.getVelocityY(), 1.0f, 1f);
		}
		return item;
	}
	
	public Inventory getInventory() {
		return inventory;
	}
	
	public Item dropSelectedItem(World world) {
		return dropItem(world, selected);
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
		this.health = Math.max(Math.min(health, getMaxHealth()), 0);
	}
}
