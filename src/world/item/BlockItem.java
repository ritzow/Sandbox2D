package world.item;

import world.block.Block;
import world.entity.component.Graphics;

public final class BlockItem extends Item {
	protected Block block;
	protected Graphics graphics;
	
	public BlockItem(Block block) {
		this.block = block;
		this.graphics = new Graphics(block.getModel());
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
		return block.getName() + " Block";
	}
	
}
