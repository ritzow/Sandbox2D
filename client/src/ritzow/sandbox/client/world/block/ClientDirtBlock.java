package ritzow.sandbox.client.world.block;

import ritzow.sandbox.client.graphics.Models;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.world.block.Block;

public class ClientDirtBlock extends ClientBlock {
	
	public ClientDirtBlock() {
		
	}
	
	public ClientDirtBlock(byte[] data) {
		
	}
	
	public ClientDirtBlock(DataReader data) {
		
	}

	@Override
	public int getModelIndex() {
		return Models.DIRT_INDEX;
	}

	@Override
	public int getHardness() {
		return 5;
	}

	@Override
	public float getFriction() {
		return 0.05f;
	}

	@Override
	public Block createNew() {
		return new ClientDirtBlock();
	}

	@Override
	public String getName() {
		return "dirt";
	}

	@Override
	public boolean isSolid() {
		return true;
	}

	@Override
	public byte[] getBytes(Serializer ser) {
		return new byte[0];
	}

}
