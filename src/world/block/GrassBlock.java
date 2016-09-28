package world.block;

import graphics.Model;
import util.ResourceManager;

public class GrassBlock extends Block {

	@Override
	public Model getModel() {
		return ResourceManager.getModel("grass");
	}

	@Override
	public int getHardness() {
		return 5;
	}
}
