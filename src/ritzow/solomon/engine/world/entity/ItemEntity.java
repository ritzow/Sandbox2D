package ritzow.solomon.engine.world.entity;

import ritzow.solomon.engine.graphics.ModelRenderer;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.world.item.Item;

public final class ItemEntity extends Entity {
	protected Item item;
	protected float rotation;
	protected float rotationSpeed;
	
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
	
	public ItemEntity(byte[] data) throws ReflectiveOperationException {
		super(data);
		item = (Item)ByteUtil.deserialize(data, 20);
		rotation = 0;
		rotationSpeed = ByteUtil.getFloat(data, 20 + ByteUtil.getSerializedLength(data, 20));
	}
	
	@Override
	public byte[] getBytes() {
		byte[] sb = super.getBytes();
		byte[] itemBytes = ByteUtil.serialize(item);
		byte[] object = new byte[sb.length + itemBytes.length + 4];
		System.arraycopy(sb, 0, object, 0, sb.length);
		System.arraycopy(itemBytes, 0, object, sb.length, itemBytes.length);
		ByteUtil.putFloat(object, sb.length + itemBytes.length, rotationSpeed);
		return object;
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
	
	public String toString() {
		return super.toString() + ", item = (" + item.toString() + "), rotation = " + rotation + ", rotationSpeed = " + rotationSpeed;
	}
}
