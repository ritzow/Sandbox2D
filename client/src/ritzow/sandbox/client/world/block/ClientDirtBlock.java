package ritzow.sandbox.client.world.block;

import ritzow.sandbox.client.graphics.RenderConstants;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.world.block.DirtBlock;

public class ClientDirtBlock extends DirtBlock implements ClientBlockProperties {
	
	public ClientDirtBlock() {}
	public ClientDirtBlock(DataReader data) {}

	@Override
	public int getModelIndex() {
		return RenderConstants.MODEL_DIRT_BLOCK;
	}

	@Override
	public boolean isTransparent() {
		return false;
	}
}
