package world.entity;

import graphics.ModelRenderer;
import world.World;
import world.item.Item;

public class ItemEntity extends Entity {
	
	private static final long serialVersionUID = 41895034901801590L;
	
	protected Item item;
	
	protected float rotation;
	protected float rotationSpeed;
	
	public ItemEntity(Item item, float x, float y) {
		this.item = item;
		this.positionX = x;
		this.positionY = y;
		this.width = 0.5f;
		this.height = 0.5f;
		this.friction = 0.02f;
		this.doBlockCollisionResolution = true;
		this.doEntityCollisionResolution = false;
		this.rotationSpeed = 0; //(float) (Math.random() < 0.5f ? -(Math.random() * 0.02f + 0.02f) : (Math.random() * 0.02f + 0.02f));
	}
	
	public void update(float milliseconds) {
		super.update(milliseconds);
		rotation = ((rotation + rotationSpeed > Math.PI * 2) ? (float)(rotation + rotationSpeed - Math.PI * 2) : rotation + rotationSpeed);
	}

	@Override
	public void render(ModelRenderer renderer) {
		renderer.loadOpacity(1.0f);
		renderer.loadTransformationMatrix(positionX, positionY, 0.5f, 0.5f, rotation);
		item.getModel().render();
	}
	
	public Item getItem() {
		return item;
	}

	@Override
	public void onCollision(World world, Entity e, float time) {
		//TODO add squash and stretch?
	}
	
}
