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

	@Override
	public boolean getShouldDelete() {
		return false;
	}

	@Override
	public boolean getDoCollision() {
		return true;
	}

	@Override
	public boolean getDoBlockCollisionResolution() {
		return true;
	}

	@Override
	public boolean getDoEntityCollisionResolution() {
		return false;
	}

	@Override
	public float getFriction() {
		return 0.02f;
	}

	@Override
	public float getWidth() {
		return 0.5f;
	}

	@Override
	public float getHeight() {
		return 0.5f;
	}

	@Override
	public float getMass() {
		return 1f;
	}
	
}
