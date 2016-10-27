package world.block;

import java.io.Serializable;

import audio.AudioSystem;
import graphics.Model;
import resource.Sounds;
import world.World;
import world.entity.ItemEntity;
import world.entity.ParticleEntity;
import world.entity.component.Graphics;
import world.item.BlockItem;

public abstract class Block implements Serializable {
	private static final long serialVersionUID = -5852957760473837301L;
	
	protected int integrity;
	
	public abstract Model getModel();
	public abstract int getHardness();
	public abstract float getFriction();
	public abstract Block createNew();
	
	public int getIntegrity() {
		return integrity;
	}
	
	public void onBreak(World world, float x, float y) {
		ItemEntity drop = new ItemEntity(new BlockItem(this), x, y);
		drop.setVelocityX(-0.2f + ((float)Math.random() * (0.4f)));
		drop.setVelocityY((float)Math.random() * (0.35f));
		world.getEntities().add(drop);
		AudioSystem.playSound(Sounds.BLOCK_BREAK, x, y, drop.getVelocityX(), drop.getVelocityY(), 1, 1);
	}
	
	public void onPlace(World world, float x, float y) {
		AudioSystem.playSound(Sounds.BLOCK_PLACE, x, y, 0, 0, 1, 1);
		for(int i = 0; i < 5; i++) {
			ParticleEntity particle = new ParticleEntity(new Graphics(getModel(), 1.0f, 0.3f, 0.3f, 0), x, y, 500, (float)Math.random() * 0.4f - 0.2f, true);
			particle.setVelocityX((float)Math.random() * 0.4f - 0.2f);
			particle.setVelocityY((float)Math.random() * 0.35f);
			world.getEntities().add(particle);
		}
	}
}
