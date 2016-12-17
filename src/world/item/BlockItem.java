package world.item;

import util.ByteUtil;
import world.block.Block;
import world.entity.component.Graphics;

public final class BlockItem extends Item {
	protected Block block;
	protected Graphics graphics;
	
	public BlockItem(byte[] data) throws ReflectiveOperationException {
		this.block = (Block)ByteUtil.deserialize(data);
		this.graphics = new Graphics(block.getModel());
	}
	
	public BlockItem(Block block) {
		this.block = block;
		this.graphics = new Graphics(block.getModel());
	}
	
	@Override
	public byte[] toBytes() {
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
