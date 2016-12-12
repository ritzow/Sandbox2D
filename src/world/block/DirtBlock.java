package world.block;

import graphics.Model;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import resource.Models;

public class DirtBlock extends Block {
	private static final long serialVersionUID = 8184830643245426503L;

	@Override
	public Model getModel() {
		return Models.DIRT_MODEL;
	}

	@Override
	public int getHardness() {
		return 5;
	}

	@Override
	public float getFriction() {
		return 0.05f;
	}

	@Override
	public Block createNew() {
		return new DirtBlock();
	}

	@Override
	public String getName() {
		return "dirt";
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
