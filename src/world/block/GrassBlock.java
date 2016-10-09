package world.block;

import graphics.Model;
import util.ResourceManager;

public class GrassBlock extends Block {
	private static final long serialVersionUID = -4649749819597297793L;

	@Override
	public Model getModel() {
		return ResourceManager.getModel("grass");
	}

	@Override
	public int getHardness() {
		return 5;
	}
}
