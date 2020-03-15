package ritzow.sandbox.client.world.item;

import ritzow.sandbox.client.graphics.Graphics;
import ritzow.sandbox.client.world.block.ClientBlockProperties;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.item.BlockItem;

public final class ClientBlockItem extends BlockItem implements Graphics {

	public ClientBlockItem(TransportableDataReader input) {
		super(input);
	}

	public ClientBlockItem(Block block) {
		super(block);
	}

	@Override
	public ClientBlockProperties getBlock() {
		return (ClientBlockProperties)block;
	}

	@Override
	public int getModelID() {
		return ((ClientBlockProperties)block).getModelIndex();
	}

	@Override
	public float getOpacity() {
		return 1;
	}

	@Override
	public float getScaleX() {
		return 1;
	}

	@Override
	public float getScaleY() {
		return 1;
	}

	@Override
	public float getRotation() {
		return 0;
	}
}
