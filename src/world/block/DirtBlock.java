package world.block;

import graphics.Model;
import util.ResourceManager;

public class DirtBlock extends Block {

	@Override
	public Model getModel() {
		return ResourceManager.getModel("dirt");
	}

	@Override
	public int getHardness() {
		return 5;
	}

}
