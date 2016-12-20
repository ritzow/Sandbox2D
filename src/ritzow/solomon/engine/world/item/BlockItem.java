package ritzow.solomon.engine.world.item;

import ritzow.solomon.engine.graphics.Graphics;
import ritzow.solomon.engine.graphics.ImmutableGraphics;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.world.block.Block;

public final class BlockItem extends Item {
	protected final Block block;
	protected final Graphics graphics;
	
	public BlockItem(byte[] data) throws ReflectiveOperationException {
		this.block = (Block)ByteUtil.deserialize(data);
		this.graphics = new ImmutableGraphics(block.getModel());
	}
	
	public BlockItem(Block block) {
		this.block = block;
		this.graphics = new ImmutableGraphics(block.getModel());
	}
	
	@Override
	public byte[] getBytes() {
		return ByteUtil.serialize(block);
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
	
}
