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

	private static final List<Light> LIGHTS_STANDING = List.of(
		new StaticLight(0, 0.5f, 0, 1, 0, 20),
		new StaticLight(0, -0.5f, 0, 0, 1, 20)
	);

	private static final List<Light> LIGHTS_CROUCHED = List.of(
		new StaticLight(0, 0, 1, 1, 1, 20)
	);


	@Override
	public Iterable<Light> lights() {
		return down ? LIGHTS_CROUCHED : LIGHTS_STANDING;
	}
}
