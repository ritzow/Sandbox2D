package world.entity;

import audio.AudioSystem;
import graphics.ModelRenderer;
import resource.Models;
import resource.Sounds;
import world.World;
import world.entity.component.Graphics;
import world.entity.component.Inventory;
import world.item.Item;

import static util.Utility.MathUtility.*;

public class Player extends LivingEntity {
	protected Inventory inventory;
	protected int selected;
	protected transient Graphics head;
	protected transient Graphics body;
	
	public Player() {
		super(100);
		this.inventory = new Inventory(9);
		this.head = new Graphics(Models.GREEN_FACE, 1.0f, 1.0f, 1.0f, 0.0f);
		this.body = new Graphics(Models.RED_SQUARE, 1.0f, 1.0f, 1.0f, 0.0f);
	}
	
	@Override
	public byte[] toBytes() {
		throw new UnsupportedOperationException("not implemented");
	}
	
	public void update(float time) {
		super.update(time);
		body.setRotation(body.getRotation() + velocityX * time); //TODO clamp to 2pi range?
	}

	@Override
	public void render(ModelRenderer renderer) {
		head.render(renderer, positionX, positionY + 0.5f);
		body.render(renderer, positionX, positionY - 0.5f);
		if(inventory.get(selected) != null) {
			inventory.get(selected)
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
				world.add(new ParticleEntity(new Graphics(i.getGraphics(), 0.75f, 0.5f, 0.5f, 0),
						positionX, positionY, randomFloat(-0.2f, 0.2f), randomFloat(0.1f, 0.3f), randomFloat(0.1f, 0.5f), randomLong(300, 1000), true));
				AudioSystem.playSound(Sounds.ITEM_PICKUP, e.getPositionX(), e.getPositionY(), 0, 0.2f, 1, (float)Math.random() * 0.4f + 0.8f);
			}
		}
		
		else if(e.getSpeed() > 0.3f) {
			new ParticleEntity(new Graphics(Models.RED_SQUARE, 0.75f, 0.5f, 0.5f, 0),
					positionX, positionY, randomFloat(-0.2f, 0.2f), randomFloat(0.1f, 0.3f), randomFloat(0.1f, 0.5f), randomLong(300, 1000), true);
			AudioSystem.playSound(Sounds.SNAP, e.positionX, e.positionY, e.velocityX, e.velocityY, 1.0f, 1.0f);
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
			AudioSystem.playSound(Sounds.THROW, positionX, positionY, entity.getVelocityX(), entity.getVelocityY(), 1.0f, 1f);
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
