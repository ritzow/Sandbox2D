package ritzow.solomon.engine.world.block;

import static ritzow.solomon.engine.util.Utility.MathUtility.randomFloat;

import ritzow.solomon.engine.audio.Audio;
import ritzow.solomon.engine.graphics.Model;
import ritzow.solomon.engine.resource.Sounds;
import ritzow.solomon.engine.util.Transportable;
import ritzow.solomon.engine.world.base.World;
import ritzow.solomon.engine.world.entity.BlockPlaceParticleEntity;
import ritzow.solomon.engine.world.entity.ItemEntity;
import ritzow.solomon.engine.world.item.BlockItem;

public abstract class Block implements Transportable {
	protected byte integrity;
	
	public Block() {
		integrity = 100;
	}
	
	public Block(byte[] data) {
		integrity = data[0];
	}
	
	public abstract String getName();
	public abstract int getModelIndex();
	public abstract int getHardness();
	public abstract float getFriction();
	public abstract boolean isSolid();
	public abstract Block createNew();
	
	public int getIntegrity() {
		return integrity;
	}
	
	public void onBreak(World world, float x, float y) {
		ItemEntity drop = new ItemEntity(new BlockItem(this.createNew()), x, y);
		drop.setVelocityX(-0.2f + ((float)Math.random() * (0.4f)));
		drop.setVelocityY((float)Math.random() * (0.35f));
		world.add(drop);
		Audio.playSound(Sounds.BLOCK_BREAK, x, y, drop.getVelocityX(), drop.getVelocityY(), randomFloat(0.75f, 1.5f), randomFloat(0.75f, 1.5f));
	}
	
	public void onPlace(World world, float x, float y) {
		Audio.playSound(Sounds.BLOCK_PLACE, x, y, 0, 0, 1, randomFloat(0.9f, 1.1f));
		for(int i = 0; i < 3; i++) {
			world.add(new BlockPlaceParticleEntity(this, x, y));
		}
	}
	
	public String toString() {
		return getName();
	}
}
