package ritzow.sandbox.client.world.item;

import ritzow.sandbox.client.graphics.Graphical;
import ritzow.sandbox.client.graphics.Graphics;
import ritzow.sandbox.client.graphics.ImmutableGraphics;
import ritzow.sandbox.client.world.block.ClientBlockProperties;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.item.BlockItem;

public final class ClientBlockItem extends BlockItem implements Graphical {
	protected final Graphics graphics;
	
	public ClientBlockItem(TransportableDataReader input) {
		super(input);
		graphics = new ImmutableGraphics(((ClientBlockProperties)block).getModelIndex(), 1.0f, 1.0f, 0.0f, 1.0f);
	}
	
	public ClientBlockItem(Block block) {
		super(block);
		this.graphics = new ImmutableGraphics(((ClientBlockProperties)block).getModelIndex(), 1.0f, 1.0f, 0.0f, 1.0f);
	}
	
	@Override
	public byte[] getBytes(Serializer ser) {
		return ser.serialize(block);
	}
	
	public ClientBlockProperties getBlock() {
		return (ClientBlockProperties)block;
	}

	public Graphics getGraphics() {
		return graphics;
	}
}
