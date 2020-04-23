package ritzow.sandbox.client.world.block;

import ritzow.sandbox.client.graphics.GameModels;
import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.world.block.DirtBlock;

public class ClientDirtBlock extends DirtBlock implements ClientBlockProperties {

	public static final ClientDirtBlock SINGLETON = new ClientDirtBlock();

	public static ClientDirtBlock getSingleton(@SuppressWarnings("unused") TransportableDataReader reader) {
		return SINGLETON;
	}

	public ClientDirtBlock() {}

	@Override
	public Model getModel() {
		return GameModels.MODEL_DIRT_BLOCK;
	}

	@Override
	public boolean isTransparent() {
		return false;
	}
}
