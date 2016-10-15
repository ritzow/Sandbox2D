package world.block;

import graphics.Model;
import util.ResourceManager;

public class DirtBlock extends Block {
	private static final long serialVersionUID = 8184830643245426503L;

	@Override
	public Model getModel() {
		return ResourceManager.DIRT_MODEL;
	}

	@Override
	public int getHardness() {
		return 5;
	}

	@Override
	public float getFriction() {
		return 0.05f;
	}

}
