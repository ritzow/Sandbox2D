package ritzow.sandbox.client.world.entity;

import ritzow.sandbox.client.graphics.Graphical;
import ritzow.sandbox.client.graphics.ModelRenderProgram;
import ritzow.sandbox.client.util.Renderable;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.item.Item;

public final class ClientItemEntity<I extends Item> extends ItemEntity<I> implements Renderable {
	protected float rotation;
	protected float rotationSpeed;
	
	//TODO get rid of constructor repetition, and maybe remove rotationSpeed field?
	public ClientItemEntity(DataReader data) {
		super(data);
		this.rotationSpeed = (float) (Math.random() < 0.5f ? -(Math.random() * 0.02f + 0.02f) : (Math.random() * 0.02f + 0.02f));
	}

	public ClientItemEntity(int entityID, I item) {
		super(entityID, item);
		this.rotationSpeed = (float) (Math.random() < 0.5f ? -(Math.random() * 0.02f + 0.02f) : (Math.random() * 0.02f + 0.02f));
	}
	
	public ClientItemEntity(int entityID, I item, float x, float y) {
		super(entityID, item);
		this.positionX = x;
		this.positionY = y;
		this.rotationSpeed = (float) (Math.random() < 0.5f ? -(Math.random() * 0.02f + 0.02f) : (Math.random() * 0.02f + 0.02f));
	}
	
	@Override
	public void update(float time) {
		super.update(time);
		rotation = ((rotation + rotationSpeed > Math.PI * 2) ? (float)(rotation + rotationSpeed * time - Math.PI * 2) : rotation + rotationSpeed * time);
	}

	@Override
	public void render(ModelRenderProgram renderer) {
		renderer.render(((Graphical)item).getGraphics(), 1.0f, positionX, positionY, 0.5f, 0.5f, rotation);
	}
	
	@Override
	public String toString() {
		return super.toString() + ", item = (" + item.toString() + "), rotation = " + rotation + ", rotationSpeed = " + rotationSpeed;
	}
}
