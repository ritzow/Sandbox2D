package ritzow.sandbox.world.item;

import java.util.Objects;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.world.block.Block;

public class BlockItem extends Item {
	protected final Block block;
	
	public BlockItem(TransportableDataReader input) {
		block = Objects.requireNonNull(input.readObject());
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
	public boolean canStack(Item item) {
		if(this == item) {
			return true;
		} else if(item instanceof BlockItem i) {
			return block.equals(i.block);
		} else {
			return false;
		}
	}
}
