package ritzow.sandbox.world.item;

import ritzow.sandbox.client.graphics.Graphics;
import ritzow.sandbox.client.graphics.ImmutableGraphics;
import ritzow.sandbox.util.ByteUtil;
import ritzow.sandbox.util.DataReader;
import ritzow.sandbox.util.Serializer;
import ritzow.sandbox.world.block.Block;

public final class BlockItem extends Item {
	protected final Block block;
	protected final Graphics graphics;
	
	public BlockItem(DataReader input) {
		block = input.readObject();
		graphics = new ImmutableGraphics(block.getModelIndex(), 1.0f, 1.0f, 1.0f, 0.0f);
	}
	
	public BlockItem(byte[] data) throws ReflectiveOperationException {
		this.block = (Block)ByteUtil.deserialize(data);
		this.graphics = new ImmutableGraphics(block.getModelIndex(), 1.0f, 1.0f, 1.0f, 0.0f);
	}
	
	public BlockItem(Block block) {
		this.block = block;
		this.graphics = new ImmutableGraphics(block.getModelIndex(), 1.0f, 1.0f, 1.0f, 0.0f);
	}
	
	@Override
	public byte[] getBytes(Serializer ser) {
		//return ByteUtil.serialize(block);
		return ser.serialize(block);
	}
	
	public Block getBlock() {
		return block;
	}

	@Override
	public Graphics getGraphics() {
		return graphics;
	}

	@Override
	public String getName() {
		return block.getName() + " block";
	}

	@Override
	public boolean equals(Item item) {
		if(this == item) {
			return true;
		} else if(item instanceof BlockItem) {
			throw new UnsupportedOperationException("not implemented");
		} else {
			return false;
		}
	}
}
