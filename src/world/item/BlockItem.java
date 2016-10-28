package world.item;

import graphics.Model;
import world.block.Block;

public class BlockItem extends Item {
	protected Block block;
	
	public BlockItem(Block block) {
		this.block = block;
	}
	
	public Block getBlock() {
		return block;
	}

	@Override
	public Model getModel() {
		return block.getModel();
	}
	
}
