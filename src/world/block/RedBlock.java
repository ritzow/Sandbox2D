package world.block;

import graphics.Model;
import resource.Models;

public class RedBlock extends Block {
	private static final long serialVersionUID = 8312467601285312374L;

	@Override
	public Model getModel() {
		return Models.RED_SQUARE;
	}

	@Override
	public int getHardness() {
		return 10;
	}

	@Override
	public float getFriction() {
		return 0.01f;
	}

	@Override
	public Block createNew() {
		return new RedBlock();
	}

	@Override
	public String getName() {
		return "Red";
	}
}
