package ritzow.solomon.engine.world.entity;

import static ritzow.solomon.engine.util.Utility.MathUtility.randomFloat;
import static ritzow.solomon.engine.util.Utility.MathUtility.randomLong;

import ritzow.solomon.engine.audio.Audio;
import ritzow.solomon.engine.graphics.Graphics;
import ritzow.solomon.engine.graphics.ImmutableGraphics;
import ritzow.solomon.engine.graphics.ModelRenderer;
import ritzow.solomon.engine.graphics.MutableGraphics;
import ritzow.solomon.engine.resource.Models;
import ritzow.solomon.engine.resource.Sounds;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.world.base.World;
import ritzow.solomon.engine.world.component.Inventory;
import ritzow.solomon.engine.world.item.Item;

/**
 * Represents a player controlled by a human
 * @author Solomon Ritzow
 *
 */
public class Player extends LivingEntity {
	protected final Graphics head;
	protected final MutableGraphics body;
	protected final Inventory inventory;
	protected volatile int selected;
	
	protected final float agility;
	protected volatile boolean left;
	protected volatile boolean right;
	protected volatile boolean up;
	protected volatile boolean down;
	
	public Player() {
		super(100);
		this.inventory = new Inventory(9);
		this.head = new ImmutableGraphics(Models.GREEN_FACE, 1.0f, 1.0f, 1.0f, 0.0f);
		this.body = new MutableGraphics(Models.RED_SQUARE, 1.0f, 1.0f, 1.0f, 0.0f);
		this.agility = 0.2f;
	}
	
	public Player(byte[] data) throws ReflectiveOperationException {
		super(data);
		this.inventory = (Inventory)ByteUtil.deserialize(data, 28);
		selected = 0;
		this.head = new ImmutableGraphics(Models.GREEN_FACE, 1.0f, 1.0f, 1.0f, 0.0f); //TODO decouple graphics from game data, this code will be messed up if run before graphics manager setup
		this.body = new MutableGraphics(Models.RED_SQUARE, 1.0f, 1.0f, 1.0f, 0.0f);
		this.agility = 0.2f; //TODO serialize agility?
	}
	
	@Override
	public byte[] getBytes() {
		byte[] superBytes = super.getBytes();
		byte[] invBytes = ByteUtil.serialize(inventory);
		byte[] bytes = new byte[superBytes.length + invBytes.length];
		ByteUtil.copy(superBytes, bytes, 0);
		ByteUtil.copy(invBytes, bytes, superBytes.length);
		return bytes;
	}
	
	public void update(float time) {
		if(left)
			velocityX = -agility;
		else if(right)
			velocityX = agility;
		if(up) {
			velocityY = agility;
			up = false;
		}
			
		positionX += velocityX * time;
		positionY += velocityY * time;
		body.setRotation(body.getRotation() + velocityX * time);
	}
	
	public float getAgility() {
		return agility;
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
		head.render(renderer, positionX, positionY + 0.5f);
		body.render(renderer, positionX, positionY - 0.5f);
		Item selectedItem = inventory.get(selected);
		if(selectedItem != null) {
			selectedItem
			.getGraphics()
			.render(renderer, positionX + velocityX * 2,  positionY, 1.0f, 0.5f, 0.5f, velocityX != 0 ? (float)Math.PI/4 * (velocityX < 0 ? -1 : 1) : 0);
		}
	}

	@Override
	public void onCollision(World world, Entity e, float time) {
		if(e instanceof ItemEntity) {
			if(inventory.add(((ItemEntity)e).getItem())) {
				Item i = ((ItemEntity)e).getItem();
				world.remove(e);
				world.add(new ParticleEntity(new ImmutableGraphics(i.getGraphics(), 0.75f, 0.5f, 0.5f, 0),
						positionX, positionY, randomFloat(-0.2f, 0.2f), randomFloat(0.1f, 0.3f), randomFloat(0.1f, 0.5f), randomLong(300, 1000), true));
				Audio.playSound(Sounds.ITEM_PICKUP, e.getPositionX(), e.getPositionY(), 0, 0.2f, 1, (float)Math.random() * 0.4f + 0.8f);
			}
		}
		
		else if(e.getSpeed() > 0.3f) {
			new ParticleEntity(new ImmutableGraphics(Models.RED_SQUARE, 0.75f, 0.5f, 0.5f, 0),
					positionX, positionY, randomFloat(-0.2f, 0.2f), randomFloat(0.1f, 0.3f), randomFloat(0.1f, 0.5f), randomLong(300, 1000), true);
			Audio.playSound(Sounds.SNAP, e.positionX, e.positionY, e.velocityX, e.velocityY, 1.0f, 1.0f);
		}
	}
	
	protected Item dropItem(World world, int slot) {
		Item item = inventory.remove(slot);
		if(item != null) {
			ItemEntity entity = new ItemEntity(item, positionX, positionY + this.getHeight());
			entity.setVelocityX((float) (Math.random() * 0.4f - 0.2f));
			entity.setVelocityY((float) (Math.random() * 0.3f));
			if(world.add(entity))
				selected = (selected == inventory.getSize() - 1 ? selected = 0 : selected + 1);
			Audio.playSound(Sounds.THROW, positionX, positionY, entity.getVelocityX(), entity.getVelocityY(), 1.0f, 1f);
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
		if(slot < inventory.getSize())
			selected = slot;
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
		return 1;
	}
}
