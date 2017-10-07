package ritzow.sandbox.world;

import ritzow.sandbox.util.Transportable;

public abstract class Block implements Transportable {
	public abstract String getName();
	public abstract int getModelIndex();
	public abstract int getHardness();
	public abstract float getFriction();
	public abstract boolean isSolid();
	public void update(World world, float time) {};
	public abstract Block createNew();
	
	public abstract void onBreak(World world, float x, float y);
	public abstract void onPlace(World world, float x, float y);
	
//	public void onBreak(World world, float x, float y) {
//		ItemEntity drop = new ItemEntity(world.nextEntityID(), new BlockItem(this.createNew()), x, y);
//		drop.setVelocityX(-0.2f + ((float)Math.random() * (0.4f)));
//		drop.setVelocityY((float)Math.random() * (0.35f));
//		world.queueAdd(drop);
//		world.getAudioSystem().playSound(Sounds.BLOCK_BREAK, x, y, drop.getVelocityX(), drop.getVelocityY(), randomFloat(0.75f, 1.5f), randomFloat(0.75f, 1.5f));
//	}
//	
//	public void onPlace(World world, float x, float y) {
//		world.getAudioSystem().playSound(Sounds.BLOCK_PLACE, x, y, 0, 0, 1, randomFloat(0.9f, 1.1f));
//	}
	
	@Override
	public String toString() {
		return getName();
	}
}
