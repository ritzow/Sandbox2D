package world.entity;

import graphics.ModelRenderer;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import world.item.Item;

public final class ItemEntity extends Entity {
	
	private static final long serialVersionUID = 41895034901801590L;
	
	protected Item item;
	protected float rotation;
	protected float rotationSpeed;
	
	public ItemEntity() {
		//for deserialization
	}
	
	public ItemEntity(Item item) {
		this.item = item;
		this.rotationSpeed = (float) (Math.random() < 0.5f ? -(Math.random() * 0.02f + 0.02f) : (Math.random() * 0.02f + 0.02f));
	}
	
	public ItemEntity(Item item, float x, float y) {
		this.item = item;
		this.positionX = x;
		this.positionY = y;
		this.rotationSpeed = (float) (Math.random() < 0.5f ? -(Math.random() * 0.02f + 0.02f) : (Math.random() * 0.02f + 0.02f));
	}
	
	public void update(float time) {
		super.update(time);
		rotation = ((rotation + rotationSpeed > Math.PI * 2) ? (float)(rotation + rotationSpeed * time - Math.PI * 2) : rotation + rotationSpeed * time);
	}

	@Override
	public void render(ModelRenderer renderer) {
		item.getGraphics().render(renderer, positionX, positionY, 1.0f, 0.5f, 0.5f, rotation);
	}
	
	public Item getItem() {
		return item;
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

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(item);
		out.writeFloat(rotation);
		out.writeFloat(rotationSpeed);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		item = (Item)in.readObject();
		rotation = in.readFloat();
		rotationSpeed = in.readFloat();
	}
	
	public String toString() {
		return super.toString() + ", item = (" + item.toString() + "), rotation = " + rotation + ", rotationSpeed = " + rotationSpeed;
	}
	
}
