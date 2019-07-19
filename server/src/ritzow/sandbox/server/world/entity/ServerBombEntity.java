package ritzow.sandbox.server.world.entity;

import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.server.network.Server;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.entity.BombEntity;

public class ServerBombEntity extends BombEntity {
	private final Server server;
	private boolean exploded;

	public ServerBombEntity(Server server, int entityID) {
		super(entityID);
		this.server = server;
	}

	public ServerBombEntity(DataReader reader) {
		super(reader);
		server = null;
	}
	
	@Override
	public void onCollision(World world, Block block, int blockX, int blockY, long nanoseconds) {
		super.onCollision(world, block, blockX, blockY, nanoseconds);
		world.getForeground().destroy(world, blockX, blockY);
		server.broadcastRemoveBlock(blockX, blockY);
		exploded = true;
	}
	
	@Override
	public boolean getShouldDelete() {
		return exploded;
	}

}
