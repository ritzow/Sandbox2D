package ritzow.sandbox.client.world.item;

import ritzow.sandbox.client.graphics.Graphics;
import ritzow.sandbox.client.graphics.ImmutableGraphics;
import ritzow.sandbox.client.world.block.ClientBlock;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.item.Item;

public final class ClientBlockItem extends ClientItem {
	protected final ClientBlock block;
	protected final Graphics graphics;
	
	public ClientBlockItem(DataReader input) {
		block = input.readObject();
		graphics = new ImmutableGraphics(block.getModelIndex(), 1.0f, 1.0f, 1.0f, 0.0f);
	}
	
	public ClientBlockItem(ClientBlock block) {
		this.block = block;
		this.graphics = new ImmutableGraphics(block.getModelIndex(), 1.0f, 1.0f, 1.0f, 0.0f);
	}
	
	@Override
	public byte[] getBytes(Serializer ser) {
		return ser.serialize(block);
	}
	
	public Block getBlock() {
		return block;
	}

	@Override
	public Graphics getGraphics() {
		return graphics;
	}

	public String getName() {
		return block.getName() + " block";
	}

	@Override
	public boolean canStack(Item item) {
		if(this == item) {
			return true;
		} else if(item instanceof ClientBlockItem) {
			throw new UnsupportedOperationException("not implemented");
		} else {
			return false;
		}
	}
}
