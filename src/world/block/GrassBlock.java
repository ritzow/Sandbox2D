package world.block;

import graphics.Model;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import resource.Models;

public class GrassBlock extends Block {
	private static final long serialVersionUID = -4649749819597297793L;

	@Override
	public Model getModel() {
		return Models.GRASS_MODEL;
	}

	@Override
	public int getHardness() {
		return 5;
	}

	@Override
	public float getFriction() {
		return 0.04f;
	}

	@Override
	public Block createNew() {
		return new GrassBlock();
	}

	@Override
	public String getName() {
		return "Grass";
	}

	@Override
	public boolean doCollision() {
		return true;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		
	}
}
