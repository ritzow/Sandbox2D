package ritzow.sandbox.server.world.entity;

import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.server.Server;
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
	public void onCollision(World world, Block block, int blockX, int blockY, float time) {
		super.onCollision(world, block, blockX, blockY, time);
		world.getForeground().destroy(world, blockX, blockY);
		server.sendRemoveBlock(blockX, blockY);
//		Utility.forEachBlock(world.getForeground(), blockX-1, blockX+1, blockY-1, blockY+1, (x, y) -> {
//			if(world.getForeground().isBlock(x, y)) {
//				world.getForeground().destroy(world, x, y);
//				server.sendRemoveBlock(x, y);
//			}
//		});
		exploded = true;
	}
	
	@Override
	public boolean getShouldDelete() {
		return exploded;
	}

}
