package world.block;

import graphics.Model;
import util.ModelManager;

public class GrassBlock extends Block {
	private static final long serialVersionUID = -4649749819597297793L;

	@Override
	public Model getModel() {
		return ModelManager.GRASS_MODEL;
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
}
