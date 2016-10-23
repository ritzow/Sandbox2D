package world.block;

import audio.Sound;
import graphics.Model;
import java.io.Serializable;
import resource.Sounds;
import world.World;
import world.entity.GenericEntity;

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
		GenericEntity particle = new GenericEntity(getModel());
		particle.setPositionX(x);
		particle.setPositionY(y);
		particle.setMass(0);
		particle.getGraphics().getRotation().setVelocity((float)Math.random() - 0.5f);
		particle.setVelocityX(-0.2f + ((float)Math.random() * (0.4f))); //min + ((float)Math.random() * (max - min))
		particle.setVelocityY((float)Math.random() * (0.35f));
		world.getEntities().add(particle);
		new Sound(Sounds.BLOCK_BREAK, x, y, particle.getVelocityX(), particle.getVelocityY(), 1, (float)(Math.random() * 0.25 + 0.75)).play(); 
		//TODO delete sounds when done, send to audio manager instead?
	}
	
	public void onPlace(World world, float x, float y) {
		for(int i = 0; i < 5; i++) {
			GenericEntity particle = new GenericEntity(getModel());
			particle.getGraphics().getScale().setX(0.2f);
			particle.getGraphics().getScale().setY(0.2f);
			particle.setPositionX(x);
			particle.setPositionY(y);
			particle.setMass(0);
			particle.getGraphics().getRotation().setVelocity((float)Math.random() - 0.5f);
			particle.setVelocityX(-0.2f + ((float)Math.random() * (0.4f))); //min + ((float)Math.random() * (max - min))
			particle.setVelocityY((float)Math.random() * (0.35f));
			world.getEntities().add(particle);
		}
	}
}
