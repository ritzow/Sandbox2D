package ritzow.solomon.engine.world.entity;

import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.world.base.ModelRenderProgram;
import ritzow.solomon.engine.world.component.Luminous;
import ritzow.solomon.engine.world.item.Item;

public final class ItemEntity extends Entity implements Luminous { //TODO temp light
	protected Item item;
	protected float rotation;
	protected float rotationSpeed;
	
	public ItemEntity(int entityID, Item item) {
		super(entityID);
		this.item = item;
		this.rotationSpeed = (float) (Math.random() < 0.5f ? -(Math.random() * 0.02f + 0.02f) : (Math.random() * 0.02f + 0.02f));
	}
	
	public ItemEntity(int entityID, Item item, float x, float y) {
		super(entityID);
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
	
	@Override
	public void update(float time) {
		super.update(time);
		rotation = ((rotation + rotationSpeed > Math.PI * 2) ? (float)(rotation + rotationSpeed * time - Math.PI * 2) : rotation + rotationSpeed * time);
	}

	@Override
	public void render(ModelRenderProgram renderer) {
		renderer.render(item.getGraphics(), 1.0f, positionX, positionY, 0.5f, 0.5f, rotation);
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
	public String toString() {
		return super.toString() + ", item = (" + item.toString() + "), rotation = " + rotation + ", rotationSpeed = " + rotationSpeed;
	}

	@Override
	public float getLightRed() {
		return 1;
	}

	@Override
	public float getLightGreen() {
		return 1;
	}

	@Override
	public float getLightBlue() {
		return 1;
	}

	@Override
	public float getLightRadius() {
		return 10;
	}

	@Override
	public float getLightIntensity() {
		return 100f;
	}
}
