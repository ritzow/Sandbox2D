package ritzow.sandbox.client.world.block;

import ritzow.sandbox.client.audio.Sounds;
import ritzow.sandbox.client.world.entity.ClientItemEntity;
import ritzow.sandbox.client.world.item.ClientBlockItem;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;

import static ritzow.sandbox.util.Utility.*;

public abstract class ClientBlock extends Block {
	public abstract String getName();
	public abstract int getModelIndex();
	
	public void onBreak(World world, float x, float y) {
		ClientItemEntity drop = new ClientItemEntity(world.nextEntityID(), new ClientBlockItem((ClientBlock)this.createNew()), x, y);
		drop.setVelocityX(-0.2f + ((float) Math.random() * (0.4f)));
		drop.setVelocityY((float) Math.random() * (0.35f));
		world.queueAdd(drop);
		world.getAudioSystem().playSound(Sounds.BLOCK_BREAK, x, y, drop.getVelocityX(), drop.getVelocityY(),
				randomFloat(0.75f, 1.5f), randomFloat(0.75f, 1.5f));
	}

	public void onPlace(World world, float x, float y) {
		world.getAudioSystem().playSound(Sounds.BLOCK_PLACE, x, y, 0, 0, 1, randomFloat(0.9f, 1.1f));
	}

}
