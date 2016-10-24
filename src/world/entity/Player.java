package world.entity;

import graphics.Renderer;
import resource.Models;
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

}
