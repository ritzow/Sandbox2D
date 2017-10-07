package ritzow.sandbox.client.world.entity;

import ritzow.sandbox.client.graphics.ModelRenderProgram;
import ritzow.sandbox.util.DataReader;
import ritzow.sandbox.world.Entity;

public abstract class ClientEntity extends Entity {

	public ClientEntity(DataReader reader) {
		super(reader);
	}

	public ClientEntity(int entityID) {
		super(entityID);
	}

	public void render(ModelRenderProgram renderer) {/* optional implementation */}
}