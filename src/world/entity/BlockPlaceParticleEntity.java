package world.entity;

import world.block.Block;
import world.entity.component.Graphics;

import static util.Utility.MathUtility.randomFloat;

public final class BlockPlaceParticleEntity extends ParticleEntity {

	private static final long serialVersionUID = -7531448862656734229L;

	public BlockPlaceParticleEntity(Block block, float posX, float posY) {
		super(new Graphics(block.getModel(), 1.0f, 0.3f, 0.3f, 0), posX, posY, randomFloat(-0.2f, 0.2f), randomFloat(0, 0.35f), randomFloat(-0.2f, 0.2f), 500, true);
	}
}
