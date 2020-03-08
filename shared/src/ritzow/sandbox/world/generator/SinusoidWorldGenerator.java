package ritzow.sandbox.world.generator;

import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.DirtBlock;
import ritzow.sandbox.world.block.GrassBlock;

public class SinusoidWorldGenerator implements WorldGenerator {

	private static final int
		DEFAULT_WORLD_WIDTH = 200,
		DEFAULT_WORLD_BASE_HEIGHT = 50,
		DEFAULT_TERRAIN_AMPLITUDE = 10,
		DEFAULT_SKY_HEIGHT = 20;
	private static final float
		DEFAULT_WORLD_FREQUENCY = 0.05f;

	private int width, baseHeight, terrainAmplitude, skyHeight;

	private float frequency;

	public static SinusoidWorldGenerator builder() {
		return new SinusoidWorldGenerator();
	}

	private SinusoidWorldGenerator() {
		this.width = DEFAULT_WORLD_WIDTH;
		this.baseHeight = DEFAULT_WORLD_BASE_HEIGHT;
		this.terrainAmplitude = DEFAULT_TERRAIN_AMPLITUDE;
		this.skyHeight = DEFAULT_SKY_HEIGHT;
		this.frequency = DEFAULT_WORLD_FREQUENCY;
	}
	public SinusoidWorldGenerator width(int width) {
		this.width = width;
		return this;
	}

	public SinusoidWorldGenerator baseHeight(int baseHeight) {
		this.baseHeight = baseHeight;
		return this;
	}

	public SinusoidWorldGenerator terrainAmplitude(int terrainAmplitude) {
		this.terrainAmplitude = terrainAmplitude;
		return this;
	}

	public SinusoidWorldGenerator skyHeight(int skyHeight) {
		this.skyHeight = skyHeight;
		return this;
	}

	public SinusoidWorldGenerator frequency(float frequency) {
		this.frequency = frequency;
		return this;
	}

	@Override
	public World generate() {
		World world = new World(width, baseHeight + terrainAmplitude + skyHeight);
		BlockGrid fg = world.getForeground();
		BlockGrid bg = world.getBackground();
		int midpoint = terrainAmplitude/2;
		int base = baseHeight - 1;
		fg.fill(DirtBlock.INSTANCE, 0, 0, fg.getWidth(), baseHeight);
		bg.fill(DirtBlock.INSTANCE, 0, 0, bg.getWidth(), baseHeight);
		for(int column = 0; column < width; ++column) {
			int max = base + midpoint + Math.round(midpoint * (float)Math.sin(column * frequency));
			for(int row = base; row <= max; ++row) {
				fg.set(column, row, DirtBlock.INSTANCE);
				bg.set(column, row, DirtBlock.INSTANCE);
			}
			fg.set(column, max, GrassBlock.INSTANCE);
			bg.set(column, max, DirtBlock.INSTANCE);
		}
		return world;
	}
}
