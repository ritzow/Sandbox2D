package world.item;

import world.block.Block;

public class BlockItem extends Item {
	protected Block block;
	
	public BlockItem(Block block) {
		this.block = block;
		this.model = block.getModel();
	}
	
	public Block getBlock() {
		return block;
	}
	
}
