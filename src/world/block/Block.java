package world.block;

import audio.AudioSystem;
import graphics.Model;
import java.io.Serializable;
import resource.Sounds;
import world.World;
import world.entity.GraphicsEntity;

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
		GraphicsEntity particle = new GraphicsEntity(getModel());
		particle.setPositionX(x);
		particle.setPositionY(y);
		particle.setMass(0);
		particle.getGraphics().setRotationVelocity((float)Math.random() - 0.5f);
		particle.setVelocityX(-0.2f + ((float)Math.random() * (0.4f))); //min + ((float)Math.random() * (max - min))
		particle.setVelocityY((float)Math.random() * (0.35f));
		world.getEntities().add(particle);
		AudioSystem.playSound(Sounds.BLOCK_BREAK, x, y, particle.getVelocityX(), particle.getVelocityY(), 1, 1);
	}
	
	public void onPlace(World world, float x, float y) {
		AudioSystem.playSound(Sounds.BLOCK_PLACE, x, y, 0, 0, 1, 1);
		for(int i = 0; i < 5; i++) {
			GraphicsEntity particle = new GraphicsEntity(getModel());
			particle.getGraphics().setScaleX(0.2f);
			particle.getGraphics().setScaleY(0.2f);
			particle.setPositionX(x);
			particle.setPositionY(y);
			particle.setMass(0);
			particle.getGraphics().setRotationVelocity((float)Math.random() - 0.5f);
			particle.setVelocityX(-0.2f + ((float)Math.random() * (0.4f))); //min + ((float)Math.random() * (max - min))
			particle.setVelocityY((float)Math.random() * (0.35f));
			world.getEntities().add(particle);
		}
	}
}
