package world.block;

import graphics.Model;
import java.io.Serializable;
import world.World;
import world.entity.GenericEntity;

public abstract class Block implements Serializable {
	private static final long serialVersionUID = -5852957760473837301L;
	
	protected int integrity;
	
	public abstract Model getModel();
	public abstract int getHardness();
	
	public void onBreak(World world, float x, float y) {
		GenericEntity particle = new GenericEntity(getModel());
		particle.position().setX(x);
		particle.position().setY(y);
		particle.getHitbox().setPriority(-1);
		particle.getGraphics().rotation().setVelocity((float)Math.random() - 0.5f);
		particle.velocity().setX(-0.2f + ((float)Math.random() * (0.4f))); //min + ((float)Math.random() * (max - min))
		particle.velocity().setY((float)Math.random() * (0.35f));
		world.getEntities().add(particle);
	}
	
	public void onPlace(World world, float x, float y) {
		for(int i = 0; i < 10; i++) {
			GenericEntity particle = new GenericEntity(getModel());
			particle.getGraphics().scale().setX(0.2f);
			particle.getGraphics().scale().setY(0.2f);
			particle.position().setX(x);
			particle.position().setY(y);
			particle.getHitbox().setPriority(-1);
			particle.getGraphics().rotation().setVelocity((float)Math.random() - 0.5f);
			particle.velocity().setX(-0.2f + ((float)Math.random() * (0.4f))); //min + ((float)Math.random() * (max - min))
			particle.velocity().setY((float)Math.random() * (0.35f));
			world.getEntities().add(particle);
		}
	}
	
	public int getIntegrity() {
		return integrity;
	}
	
}
