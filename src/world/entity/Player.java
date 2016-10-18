package world.entity;

import graphics.Renderer;
import world.item.Item;

public class Player extends LivingEntity {

	private static final long serialVersionUID = 4619416956992212820L;
	
	protected final Item[] inventory;
	
	public Player(int inventorySize) {
		super(100);
		inventory = new Item[inventorySize];
	}

	@Override
	public void render(Renderer renderer) {
		
	}

}
