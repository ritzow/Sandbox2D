package ritzow.solomon.engine.world.entity;

import static ritzow.solomon.engine.util.Utility.MathUtility.randomFloat;

import ritzow.solomon.engine.graphics.MutableGraphics;
import ritzow.solomon.engine.world.block.Block;

public final class BlockPlaceParticleEntity extends ParticleEntity {
	public BlockPlaceParticleEntity(Block block, float posX, float posY) {
		super(new MutableGraphics(block.getModel(), 1.0f, 0.3f, 0.3f, 0), posX, posY, randomFloat(-0.2f, 0.2f), randomFloat(0, 0.35f), randomFloat(-0.2f, 0.2f), 500, true);
	}
}
