package ritzow.sandbox.client.world.entity;

import ritzow.sandbox.client.graphics.GameModels;
import ritzow.sandbox.client.graphics.ModelRenderer;
import ritzow.sandbox.client.graphics.Renderable;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.world.entity.PlayerEntity;

/**
 * Represents a player
 * @author Solomon Ritzow
 */
public class ClientPlayerEntity extends PlayerEntity implements Renderable {

	public ClientPlayerEntity(int entityID) {
		super(entityID);
	}

	public ClientPlayerEntity(TransportableDataReader input) {
		super(input);
	}

	@Override
	public void render(ModelRenderer renderer, float exposure) {
		float positionX = this.positionX;
		float positionY = this.positionY;
		renderer.queueRender(
			GameModels.MODEL_GREEN_FACE,
			1.0f,
			exposure,
			positionX,
			positionY + (down ? 0 : SIZE_SCALE / 2),
			SIZE_SCALE,
			SIZE_SCALE,
			0.0f
		);
		if(!down) {
			renderer.queueRender(
				GameModels.MODEL_RED_SQUARE,
				1.0f,
				exposure,
				positionX,
				positionY - SIZE_SCALE / 2,
				SIZE_SCALE,
				SIZE_SCALE,
				//positionX/SIZE_SCALE
				0
			);
		}

//		if(inventory.isItem(selected)) {
//			renderer.render(((Graphical) inventory.get(selected)).getGraphics().getModelID(),
//					1.0f, positionX, positionY, 0.5f * SIZE_SCALE, 0.5f * SIZE_SCALE, 0);
//		}
	}
}
