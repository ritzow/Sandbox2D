package ritzow.sandbox.client.world.block;

import ritzow.sandbox.client.graphics.GameModels;
import ritzow.sandbox.client.graphics.Light;
import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.client.graphics.StaticLight;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.world.block.GlassBlock;

public class ClientGlassBlock extends GlassBlock implements ClientBlockProperties {
	private static final ClientGlassBlock INSTANCE = new ClientGlassBlock();
	private static final Light LIGHTING = new StaticLight(0, 0, 1, 1, 1, 1); //TODO the posX and posY here need to be used as offsets when adding this to the lights list

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

	@Override
	public Light lighting() {
		return LIGHTING;
	}
}
