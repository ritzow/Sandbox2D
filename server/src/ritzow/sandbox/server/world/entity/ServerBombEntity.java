package ritzow.sandbox.server.world.entity;

import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.server.network.GameServer;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.entity.BombEntity;

public class ServerBombEntity extends BombEntity {
	private final GameServer server;
	private boolean exploded;

	public ServerBombEntity(GameServer server, int entityID) {
		super(entityID);
		this.server = server;
	}

	public ServerBombEntity(DataReader reader) {
		super(reader);
		server = null;
	}
	
	@Override
	public void onCollision(World world, Block block, int blockX, int blockY, float time) {
		super.onCollision(world, block, blockX, blockY, time);
		world.getForeground().destroy(world, blockX, blockY);
		server.sendRemoveBlock(blockX, blockY);
		exploded = true;
	}
	
	@Override
	public boolean getShouldDelete() {
		return exploded;
	}

}
