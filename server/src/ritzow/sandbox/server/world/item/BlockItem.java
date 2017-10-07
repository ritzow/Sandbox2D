package ritzow.sandbox.server.world.item;

import ritzow.sandbox.util.DataReader;
import ritzow.sandbox.util.Serializer;
import ritzow.sandbox.world.Item;

public class BlockItem extends Item {
	
	public BlockItem(DataReader data) {
		super(data);
	}

	@Override
	public byte[] getBytes(Serializer ser) {
		//TODO implement method 'getBytes'
		return null;
	}

	@Override
	public String getName() {
		//TODO implement method 'getName'
		return null;
	}

	@Override
	public boolean equals(Item item) {
		//TODO implement method 'equals'
		return false;
	}

}
