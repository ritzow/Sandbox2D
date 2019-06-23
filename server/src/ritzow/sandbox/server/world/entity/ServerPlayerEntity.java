package ritzow.sandbox.server.world.entity;

import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.entity.PlayerEntity;

public class ServerPlayerEntity extends PlayerEntity {

	public ServerPlayerEntity(int entityID) {
		super(entityID);
	}

	public ServerPlayerEntity(TransportableDataReader input) {
		super(input);
	}

	@Override
	public void onCollision(World world, Entity e, float time) {
		if(e instanceof ItemEntity && e.getVelocityY() > -0.05f && e.getVelocityY() <= 0) {
			float vx = Utility.randomFloat(-0.15f, 0.15f);
			e.setVelocityX(vx);
			e.setVelocityY(Utility.maxComponentInRadius(vx, 0.5f));
		}
	}

	@Override
	public boolean hasEntityCollisionLogic() {
		return true;
	}
}
