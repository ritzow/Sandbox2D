package ritzow.sandbox.client.world.block;

import ritzow.sandbox.client.graphics.GameModels;
import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.world.block.GlassBlock;

public class ClientGlassBlock extends GlassBlock implements ClientBlockProperties {
	private static final ClientGlassBlock INSTANCE = new ClientGlassBlock();

	public static ClientGlassBlock getSingleton(TransportableDataReader transportableDataReader) {
		return INSTANCE;
	}

	@Override
	public Model getModel() {
		return GameModels.MODEL_GLASS_BLOCK;
	}

	@Override
	public boolean isTransparent() {
		return true;
	}
}
