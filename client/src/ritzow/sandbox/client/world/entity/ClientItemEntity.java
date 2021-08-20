package ritzow.sandbox.client.world.entity;

import java.util.random.RandomGeneratorFactory;
import ritzow.sandbox.client.graphics.Graphics;
import ritzow.sandbox.client.graphics.ModelRenderer;
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
		this.rotationVelocity = Utility.convertPerSecondToPerNano(
			(float)RandomGeneratorFactory.getDefault().create().nextDouble(-Math.PI, Math.PI));
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
}
