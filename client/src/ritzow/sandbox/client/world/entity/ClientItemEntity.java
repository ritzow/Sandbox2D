package ritzow.sandbox.client.world.entity;

import ritzow.sandbox.client.graphics.Graphics;
import ritzow.sandbox.client.graphics.ModelRenderProgram;
import ritzow.sandbox.client.graphics.Renderable;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.item.Item;

public final class ClientItemEntity<I extends Item> extends ItemEntity<I> implements Renderable {
	private final float rotationVelocity;
	private float rotation;
	
	public ClientItemEntity(TransportableDataReader data) {
		super(data);
		this.rotationVelocity = getRotationalVelocity();
	}
	
	public ClientItemEntity(int entityID, I item, float x, float y) {
		super(entityID, item);
		this.positionX = x;
		this.positionY = y;
		this.rotationVelocity = getRotationalVelocity();
		this.rotation = 0;
	}
	
	private static float getRotationalVelocity() {
		return Utility.convertPerSecondToPerNano(Utility.random(-Math.PI, Math.PI));
	}
	
	@Override
	public void update(World world, long ns) {
		super.update(world, ns);
		this.rotation = Math.fma(ns, rotationVelocity, this.rotation);
	}

	@Override
	public void render(ModelRenderProgram renderer) {
		Graphics g = ((Graphics)item);
		renderer.queueRender(
			g.getModelID(), 
			g.getOpacity(), 
			positionX, 
			positionY, 
			g.getScaleX() * 0.5f,
			g.getScaleY() * 0.5f,
			g.getRotation() + rotation
		);
	}
	
	@Override
	public String toString() {
		return super.toString() + ", item = (" + item + ")";
	}
}
