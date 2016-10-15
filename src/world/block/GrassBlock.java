package world.block;

import graphics.Model;
import util.ResourceManager;

public class GrassBlock extends Block {
	private static final long serialVersionUID = -4649749819597297793L;

	@Override
	public Model getModel() {
		return ResourceManager.GRASS_MODEL;
	}

	@Override
	public int getHardness() {
		return 5;
	}

	@Override
	public float getFriction() {
		return 0.04f;
	}
}
