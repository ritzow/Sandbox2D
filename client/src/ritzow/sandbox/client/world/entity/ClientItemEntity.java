package ritzow.sandbox.client.world.entity;

import ritzow.sandbox.client.graphics.Graphical;
import ritzow.sandbox.client.graphics.ModelRenderProgram;
import ritzow.sandbox.client.graphics.Renderable;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.item.Item;

public final class ClientItemEntity<I extends Item> extends ItemEntity<I> implements Renderable {
	private float rotation;
	
	private static final float ROTATION_SPEED = 0.05f;

	public ClientItemEntity(int entityID, I item) {
		this(entityID, item, (float) (Math.random() * Math.PI * 2), 0, 0);
	}
	
	public ClientItemEntity(DataReader data) {
		super(data);
		this.rotation = (float) (Math.random() * Math.PI * 2);
	}
	
	public ClientItemEntity(int entityID, I item, float rotation, float x, float y) {
		super(entityID, item);
		this.rotation = rotation;
		this.positionX = x;
		this.positionY = y;
	}
	
	@Override
	public void update(World world, float time) {
		super.update(world, time);
		rotation = Math.abs(rotation + ROTATION_SPEED * time);
	}

	@Override
	public void render(ModelRenderProgram renderer) {
		renderer.render(((Graphical)item).getGraphics(), 1.0f, positionX, positionY, 0.5f, 0.5f, rotation);
	}
	
	@Override
	public String toString() {
		return super.toString() + ", item = (" + item.toString() + "), rotation = " + rotation;
	}
}
