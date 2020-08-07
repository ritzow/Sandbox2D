package ritzow.sandbox.client.world.entity;

import java.util.List;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.world.entity.PlayerEntity;

/**
 * Represents a player
 * @author Solomon Ritzow
 */
public class ClientPlayerEntity extends PlayerEntity implements Renderable, Lit {
	public ClientPlayerEntity(int entityID) {
		super(entityID);
	}

	public ClientPlayerEntity(TransportableDataReader input) {
		super(input);
	}

	@Override
	public void render(ModelRenderer renderer, float exposure) {
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
				GameModels.MODEL_BLUE_SQUARE,
				1.0f,
				exposure,
				positionX,
				positionY - SIZE_SCALE / 2,
				SIZE_SCALE,
				SIZE_SCALE,
				0
			);
		}
	}

	@Override
	public Iterable<Light> lights() {
		return List.of(new Light() {

			@Override
			public float posX() {
				return 0;
			}

			@Override
			public float posY() {
				return 0;
			}

			@Override
			public float red() {
				return 0.0f;
			}

			@Override
			public float green() {
				return 1.0f;
			}

			@Override
			public float blue() {
				return 0.0f;
			}

			@Override
			public float intensity() {
				return 5f;
			}
		});
	}
}
