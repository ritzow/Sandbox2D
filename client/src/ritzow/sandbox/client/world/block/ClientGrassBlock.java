package ritzow.sandbox.client.world.block;

import ritzow.sandbox.client.graphics.RenderConstants;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.world.block.GrassBlock;

public class ClientGrassBlock extends GrassBlock implements ClientBlockProperties {
	
	public static final ClientGrassBlock SINGLETON = new ClientGrassBlock();
	
	public static final ClientGrassBlock getSingleton(@SuppressWarnings("unused") TransportableDataReader reader) {
		return SINGLETON;
	}

	@Override
	public int getModelIndex() {
		return RenderConstants.MODEL_GRASS_BLOCK;
	}

	@Override
	public boolean isTransparent() {
		return false;
	}
}
