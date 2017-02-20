package ritzow.solomon.engine.world.block;

import static ritzow.solomon.engine.util.Utility.MathUtility.randomFloat;

import ritzow.solomon.engine.audio.Sounds;
import ritzow.solomon.engine.util.Transportable;
import ritzow.solomon.engine.world.base.World;
import ritzow.solomon.engine.world.entity.ItemEntity;
import ritzow.solomon.engine.world.item.BlockItem;

public abstract class Block implements Transportable {
	public abstract String getName();
	public abstract int getModelIndex();
	public abstract int getHardness();
	public abstract float getFriction();
	public abstract boolean isSolid();
	public abstract Block createNew();
	
	public void onBreak(World world, float x, float y) {
		ItemEntity drop = new ItemEntity(0, new BlockItem(this.createNew()), x, y); //TODO deal with entityID for entities created outside server directly
		drop.setVelocityX(-0.2f + ((float)Math.random() * (0.4f)));
		drop.setVelocityY((float)Math.random() * (0.35f));
		world.add(drop);
		world.getAudioSystem().playSound(Sounds.BLOCK_BREAK, x, y, drop.getVelocityX(), drop.getVelocityY(), randomFloat(0.75f, 1.5f), randomFloat(0.75f, 1.5f));
	}
	
	public void onPlace(World world, float x, float y) {
		world.getAudioSystem().playSound(Sounds.BLOCK_PLACE, x, y, 0, 0, 1, randomFloat(0.9f, 1.1f));
	}
	
	public String toString() {
		return getName();
	}
}
