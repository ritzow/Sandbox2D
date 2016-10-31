package world.entity;

import audio.AudioSystem;
import graphics.Renderer;
import resource.Models;
import resource.Sounds;
import world.World;
import world.entity.component.Graphics;
import world.entity.component.Inventory;
import world.item.Item;

public class Player extends LivingEntity {

	private static final long serialVersionUID = 4619416956992212820L;
	
	protected final String username;
	protected final Inventory inventory;
	protected int selected;
	
	protected Graphics head;
	protected Graphics body;
	
	public Player(String username) {
		super(100);
		this.mass = 1.0f;
		this.friction = 0.02f;
		this.width = 1.0f;
		this.height = 2.0f;
		this.head = new Graphics(Models.GREEN_FACE, 1.0f, 1.0f, 1.0f, 0.0f);
		this.body = new Graphics(Models.BLUE_SQUARE, 1.0f, 1.0f, 1.0f, 0.0f);
		this.username = username;
		this.inventory = new Inventory(9);
	}
	
	public void update(float milliseconds) {
		super.update(milliseconds);
		body.setRotation(body.getRotation() + velocityX); //TODO clamp to 2pi range
	}

	@Override
	public void render(Renderer renderer) {
		head.render(renderer, positionX, positionY + 0.5f);
		body.render(renderer, positionX, positionY - 0.5f);
		if(inventory.get(selected) != null) {
			renderer.loadOpacity(1.0f);
			renderer.loadTransformationMatrix(positionX + velocityX * 2, positionY, 0.5f, 0.5f, velocityX != 0 ? (float)Math.PI/4 * (velocityX < 0 ? -1 : 1) : 0);
			inventory.get(selected).getModel().render();
		}
	}

	@Override
	public void onCollision(World world, Entity e) {
		if(e instanceof ItemEntity && inventory.add(((ItemEntity)e).getItem())) {
			world.getEntities().remove(e);
			world.getEntities().add(new ParticleEntity(new Graphics(((ItemEntity)e).getItem().getModel(), 1.0f, 0.5f, 0.5f, 0), e.getPositionX(), e.getPositionY(), 0, 0.2f, 500, 0, true));
			AudioSystem.playSound(Sounds.ITEM_PICKUP, e.getPositionX(), e.getPositionY(), 0, 0.2f, 1, (float)Math.random() * 0.4f + 0.8f);
		}
	}
	
	public Item dropItem(World world, int slot) {
		Item item = inventory.remove(slot);
		if(item != null) {
			ItemEntity entity = new ItemEntity(item, positionX, positionY + height);
			entity.setVelocityX((float) (Math.random() * 0.4f - 0.2f));
			entity.setVelocityY((float) (Math.random() * 0.3f));
			if(world.getEntities().add(entity)) {
				selected = (selected == inventory.getSize() - 1 ? selected = 0 : selected + 1);
			}
			AudioSystem.playSound(Sounds.THROW, positionX, positionY, entity.getVelocityX(), entity.getVelocityY(), 1.0f, 1f);
		}
		
		return item;
	}
	
	public String getName() {
		return username;
	}
	
	public Inventory getInventory() {
		return inventory;
	}
	
	public int getSelectedSlot() {
		return selected;
	}
	
	public void setSelected(int slot) {
		selected = slot;
	}

}
