package ritzow.sandbox.client.world.entity;

import ritzow.sandbox.client.graphics.ModelRenderProgram;
import ritzow.sandbox.client.graphics.RenderConstants;
import ritzow.sandbox.client.graphics.Renderable;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.world.entity.BombEntity;

public class ClientBombEntity extends BombEntity implements Renderable {

	public ClientBombEntity(int entityID) {
		super(entityID);
	}

	public ClientBombEntity(DataReader reader) {
		super(reader);
	}

	@Override
	public void render(ModelRenderProgram program) {
		program.queueRender(
			RenderConstants.MODEL_RED_SQUARE, 
			1.0f, 
			positionX, 
			positionY, 
			getWidth(),
			getHeight(), 
			positionX
		);
	}

}
