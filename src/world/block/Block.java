package world.block;

import graphics.Model;
import world.World;
import world.entity.Entity;

public abstract class Block {
	protected int integrity;
	
	public abstract Model getModel();
	public abstract int getHardness();
	
	public void onBreak(World world, float x, float y) {
		Entity particle = new Entity(getModel());
		particle.position().setX(x);
		particle.position().setY(y);
		particle.hitbox().setPriority(-1);
		particle.graphics().rotation().setVelocity((float)Math.random() - 0.5f);
		particle.velocity().setX(-0.2f + ((float)Math.random() * (0.4f))); //min + ((float)Math.random() * (max - min))
		particle.velocity().setY((float)Math.random() * (0.35f));
		world.getEntities().add(particle);
	}
	
	public void onPlace(World world, float x, float y) {
		for(int i = 0; i < 10; i++) {
			Entity particle = new Entity(getModel());
			particle.graphics().scale().setX(0.2f);
			particle.graphics().scale().setY(0.2f);
			particle.position().setX(x);
			particle.position().setY(y);
			particle.hitbox().setPriority(-1);
			particle.graphics().rotation().setVelocity((float)Math.random() - 0.5f);
			particle.velocity().setX(-0.2f + ((float)Math.random() * (0.4f))); //min + ((float)Math.random() * (max - min))
			particle.velocity().setY((float)Math.random() * (0.35f));
			world.getEntities().add(particle);
		}
	}
	
	public int getIntegrity() {
		return integrity;
	}
}
