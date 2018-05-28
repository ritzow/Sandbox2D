package ritzow.sandbox.client.world.block;

import ritzow.sandbox.data.ByteUtil;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.client.graphics.RenderConstants;

public class ClientDirtBlock extends ClientBlock {
	
	public ClientDirtBlock() {
		
	}
	
	public ClientDirtBlock(DataReader data) {
		
	}

	@Override
	public int getModelIndex() {
		return RenderConstants.MODEL_DIRT_BLOCK;
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
	public String getName() {
		return "dirt";
	}

	@Override
	public boolean isSolid() {
		return true;
	}

	@Override
	public byte[] getBytes(Serializer ser) {
		return ByteUtil.EMPTY_BYTE_ARRAY;
	}

	@Override
	public boolean isTransparent() {
		return false;
	}

}
