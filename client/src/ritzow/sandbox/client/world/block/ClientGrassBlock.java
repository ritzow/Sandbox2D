package ritzow.sandbox.client.world.block;

import ritzow.sandbox.data.ByteUtil;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.client.graphics.RenderConstants;

public class ClientGrassBlock extends ClientBlock {
	
	public ClientGrassBlock() {
		
	}
	
	public ClientGrassBlock(DataReader input) {
		
	}

	@Override
	public int getModelIndex() {
		return RenderConstants.MODEL_GRASS_BLOCK;
	}

	@Override
	public int getHardness() {
		return 5;
	}

	@Override
	public float getFriction() {
		return 0.04f;
	}

	@Override
	public String getName() {
		return "Grass";
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
