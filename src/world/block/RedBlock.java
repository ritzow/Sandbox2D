package world.block;

import graphics.Model;
import util.ResourceManager;

public class RedBlock extends Block {
	private static final long serialVersionUID = 9112806922906010577L;

	public RedBlock() {
		this.integrity = 100;
	}

	@Override
	public Model getModel() {
		return ResourceManager.getModel("red_square");
	}

	@Override
	public int getHardness() {
		return 10;
	}

	@Override
	public float getFriction() {
		return 0.01f;
	}
}
