package world.block;

import graphics.Model;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import resource.Models;
import world.World;

public class AirBlock extends Block {

	private static final long serialVersionUID = 1L;
	//TODO should I implement air density? friction value changes based on current air density/player suffocates when air density is too low
	@Override
	public String getName() {
		return "air";
	}

	@Override
	public Model getModel() {
		return Models.BLUE_SQUARE;
	}

	@Override
	public int getHardness() {
		return 0;
	}

	@Override
	public float getFriction() {
		return 0.5f;
	}
	
	@Override
	public void onBreak(World world, float x, float y) {
		
	}

	@Override
	public void onPlace(World world, float x, float y) {
		
	}

	@Override
	public Block createNew() {
		return new AirBlock();
	}

	@Override
	public boolean doCollision() {
		return false;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		
	}
}
