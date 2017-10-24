package ritzow.sandbox.client.world.entity;

import ritzow.sandbox.client.graphics.Graphical;
import ritzow.sandbox.client.graphics.ModelRenderProgram;
import ritzow.sandbox.client.graphics.Models;
import ritzow.sandbox.client.util.Renderable;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.world.component.Luminous;
import ritzow.sandbox.world.entity.PlayerEntity;

/**
 * Represents a player controlled by a human
 * @author Solomon Ritzow
 */
public class ClientPlayerEntity extends PlayerEntity implements Luminous, Renderable {
	
	public ClientPlayerEntity(int entityID) {
		super(entityID);
	}
	
	public ClientPlayerEntity(DataReader input) {
		super(input);
	}

	@Override
	public void render(ModelRenderProgram renderer) {
		renderer.render(Models.forIndex(Models.GREEN_FACE_INDEX), 1.0f, positionX, positionY + 0.5f, 1.0f, 1.0f, 0.0f);
		renderer.render(Models.forIndex(Models.RED_SQUARE_INDEX), 1.0f, positionX, positionY - 0.5f, 1.0f, 1.0f, positionX);
		
		Graphical selectedItem = (Graphical)inventory.get(selected);
		if(selectedItem != null) {
			renderer.render(Models.forIndex(selectedItem.getGraphics().getModelIndex()), 1.0f, positionX, positionY, 0.5f, 0.5f, 0);
					//velocityX != 0 ? (float)Math.PI/4 * (velocityX < 0 ? -1 : 1) : 0);
		}
	}

	@Override
	public float getLightRed() {
		return 1.0f;
	}

	@Override
	public float getLightGreen() {
		return 0.0f;
	}

	@Override
	public float getLightBlue() {
		return 1.0f;
	}

	@Override
	public float getLightRadius() {
		return 50;
	}

	@Override
	public float getLightIntensity() {
		return 100;
	}
}
