package ritzow.sandbox.server.world.entity;

import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.entity.PlayerEntity;

public class ServerPlayerEntity extends PlayerEntity {
	private static final float LAUNCH_VELOCITY = Utility.convertPerSecondToPerNano(10f);

	public ServerPlayerEntity(int entityID) {
		super(entityID);
	}

	public ServerPlayerEntity(TransportableDataReader input) {
		super(input);
	}

	@Override
	public void onCollision(World world, Entity e, long nanoseconds) {
		if(e instanceof ItemEntity && e.getVelocityY() <= 0) {
			Utility.launchAtRandomRatio(e, 1/8d, 3/8d, LAUNCH_VELOCITY * Utility.random(1, 2));
		}
	}

	@Override
	public boolean hasCollision() {
		return true;
	}
}
