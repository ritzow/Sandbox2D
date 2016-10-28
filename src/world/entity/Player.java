package world.entity;

import audio.AudioSystem;
import graphics.Renderer;
import resource.Models;
import resource.Sounds;
import world.World;
import world.entity.component.Graphics;
import world.item.Item;

public class Player extends LivingEntity {

	private static final long serialVersionUID = 4619416956992212820L;
	
	protected final Item[] inventory;
	
	protected Graphics head;
	protected Graphics body;
	
	public Player() {
		super(100);
		this.mass = 1.0f;
		this.friction = 0.02f;
		this.width = 1.0f;
		this.height = 2.0f;
		this.head = new Graphics(Models.GREEN_FACE, 1.0f, 1.0f, 1.0f, 0.0f);
		this.body = new Graphics(Models.BLUE_SQUARE, 1.0f, 1.0f, 1.0f, 0.0f);
		this.inventory = new Item[20];
	}
	
	public void update(float milliseconds) {
		super.update(milliseconds);
		body.setRotation(body.getRotation() + velocityX);
	}

	@Override
	public void render(Renderer renderer) {
		head.render(renderer, positionX, positionY + 0.5f);
		body.render(renderer, positionX, positionY - 0.5f);
	}

	@Override
	public void onCollision(World world, Entity e) {
		if(e instanceof ItemEntity) {
			for(int i = 0; i < inventory.length; i++) {
				if(inventory[i] == null) {
					inventory[i] = ((ItemEntity)e).getItem();
					world.getEntities().remove(e);
					world.getEntities().add(
							new ParticleEntity(new Graphics(((ItemEntity)e).getItem().getModel(), 1.0f, 0.5f, 0.5f, 0), 
									e.getPositionX(), e.getPositionY(), 0, 0.2f, 500, (float)Math.random() * 0.4f - 0.2f, true));
					AudioSystem.playSound(Sounds.BLOOP, e.getPositionX(), e.getPositionY(), 0, 0.2f, 1, (float)Math.random() * 0.4f + 0.8f);
					System.out.println("added item to slot " + (i + 1));
					return;
				}
			AudioSystem.playSound(Sounds.ITEM_PICKUP, e.getPositionX(), e.getPositionY(), 0, 0.2f, 1, (float)Math.random() * 0.4f + 0.8f);
			}
		}
	}

}
