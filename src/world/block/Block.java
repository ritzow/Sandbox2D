package world.block;

import static audio.AudioSystem.playSound;
import static util.Utility.MathUtility.randomFloat;

import graphics.Model;
import java.io.Serializable;
import resource.Sounds;
import world.World;
import world.entity.BlockPlaceParticleEntity;
import world.entity.ItemEntity;
import world.item.BlockItem;

public abstract class Block implements Serializable {
	private static final long serialVersionUID = -5852957760473837301L;
	
	protected byte integrity;
	
	{
		integrity = 100;
	}
	
	public abstract String getName();
	public abstract Model getModel();
	public abstract int getHardness();
	public abstract float getFriction();
	public abstract Block createNew();
	
	public int getIntegrity() {
		return integrity;
	}
	
	public void onBreak(World world, float x, float y) {
		ItemEntity drop = new ItemEntity(new BlockItem(this.createNew()), x, y);
		drop.setVelocityX(-0.2f + ((float)Math.random() * (0.4f)));
		drop.setVelocityY((float)Math.random() * (0.35f));
		world.getEntities().add(drop);
		playSound(Sounds.BLOCK_BREAK, x, y, drop.getVelocityX(), drop.getVelocityY(), randomFloat(0.75f, 1.5f), randomFloat(0.75f, 1.5f));
	}
	
	public void onPlace(World world, float x, float y) {
		playSound(Sounds.BLOCK_PLACE, x, y, 0, 0, 1, randomFloat(0.9f, 1.1f));
		for(int i = 0; i < 3; i++) {
			world.getEntities().add(new BlockPlaceParticleEntity(this, x, y));
		}
	}
}
