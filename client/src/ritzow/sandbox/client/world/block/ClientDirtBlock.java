package ritzow.sandbox.client.world.block;

import ritzow.sandbox.client.graphics.RenderConstants;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.world.block.DirtBlock;

public class ClientDirtBlock extends DirtBlock implements ClientBlockProperties {
	
	public static final ClientDirtBlock SINGLETON = new ClientDirtBlock();
	
	public static final ClientDirtBlock getSingleton(@SuppressWarnings("unused") TransportableDataReader reader) {
		return SINGLETON;
	}
	
	public ClientDirtBlock() {}

	@Override
	public int getModelIndex() {
		return RenderConstants.MODEL_DIRT_BLOCK;
	}

	@Override
	public boolean isTransparent() {
		return false;
	}
}
