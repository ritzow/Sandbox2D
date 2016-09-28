package world.block;

import graphics.Model;
import util.ResourceManager;

public class RedBlock extends Block {
	
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
}
