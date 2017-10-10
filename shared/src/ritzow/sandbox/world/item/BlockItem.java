package ritzow.sandbox.world.item;

import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.item.Item;

public final class BlockItem extends Item {
	protected final Block block;
	
	public BlockItem(DataReader input) {
		block = input.readObject();
	}
	
	public BlockItem(Block block) {
		this.block = block;
	}
	
	@Override
	public byte[] getBytes(Serializer ser) {
		return ser.serialize(block);
	}
	
	public Block getBlock() {
		return block;
	}
	
	@Override
	public String getName() {
		return block.getName() + " block";
	}

	@Override
	public boolean canStack(Item item) {
		if(this == item) {
			return true;
		} else if(item instanceof BlockItem) {
			throw new UnsupportedOperationException("not implemented");
		} else {
			return false;
		}
	}
}
