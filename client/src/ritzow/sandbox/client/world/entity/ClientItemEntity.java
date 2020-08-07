package ritzow.sandbox.client.world.entity;

import java.util.List;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.item.Item;

public final class ClientItemEntity<I extends Item> extends ItemEntity<I> implements Renderable, Lit {
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
	public void render(ModelRenderer renderer, float exposure) {
		Graphics g = ((Graphics)item);
		renderer.queueRender(
			g.getModel(),
			g.getOpacity(),
			exposure,
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

	@Override
	public Iterable<Light> lights() {
		return List.of(new StaticLight(0, 0, 1, 0, 0, 4f));
	}
}
