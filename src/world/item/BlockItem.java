package world.item;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import world.block.Block;
import world.entity.component.Graphics;

public final class BlockItem extends Item {
	protected Block block;
	protected Graphics graphics;
	
	public BlockItem() {
		
	}
	
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

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(block);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		block = (Block)in.readObject();
		this.graphics = new Graphics(block.getModel());
	}
	
}
