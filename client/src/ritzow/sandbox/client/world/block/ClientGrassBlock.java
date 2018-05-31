package ritzow.sandbox.client.world.block;

import ritzow.sandbox.client.graphics.RenderConstants;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.world.block.GrassBlock;

public class ClientGrassBlock extends GrassBlock implements ClientBlockProperties {
	
	public ClientGrassBlock() {}
	
	public ClientGrassBlock(DataReader input) {}

	@Override
	public int getModelIndex() {
		return RenderConstants.MODEL_GRASS_BLOCK;
	}

	@Override
	public boolean isTransparent() {
		return false;
	}
}
