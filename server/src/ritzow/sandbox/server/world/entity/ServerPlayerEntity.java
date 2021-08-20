package ritzow.sandbox.server.world.entity;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.entity.PlayerEntity;

public class ServerPlayerEntity extends PlayerEntity {
	private static final float LAUNCH_VELOCITY = Utility.convertPerSecondToPerNano(10f);

	private final RandomGenerator random;

	public ServerPlayerEntity(int entityID) {
		super(entityID);
		this.random = RandomGeneratorFactory.of("L64X128MixRandom").create();
	}

	@Override
	public void onCollision(World world, Entity e, long nanoseconds) {
		if(e instanceof ItemEntity && e.getVelocityY() <= 0) {
			Utility.launchAtRandomRatio(random, e, 1/8d, 3/8d, LAUNCH_VELOCITY * random.nextFloat(1, 2));
			e.setVelocityX(e.getVelocityX() + this.velocityX);
		}
	}

	@Override
	public boolean interactsWithEntities() {
		return true;
	}
}
