package ritzow.sandbox.client.world;

import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.client.world.block.ClientBlockProperties;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.entity.Entity;

public final class ClientWorldRenderer {
	private final ModelRenderer modelProgram;
	private final World world;
	private final float[] lighting;
	private final Camera camera;

	public ClientWorldRenderer(ModelRenderer modelProgram, Camera camera, World world) {
		this.modelProgram = modelProgram;
		this.camera = camera;
		this.world = world;
		this.lighting = new float[world.getBlocks().getWidth() * world.getBlocks().getHeight()];
		initLighting();
		GraphicsUtility.checkErrors();
	}

	private static final float LIGHT_PENETRATION = 0.75f, LIGHT_DISSIPATION = 0.9f;
	private static final int SUNLIGHT_RADIUS = 10;

	public static final float LAYER_EXPOSURE_FACTOR = 0.5f;

	private void initLighting() {
		//when a background and foreground block does not exist, light is emitted in a radius around it (depending on opacity potentially).
		//light level is reduced to near zero over radius, or faster if foreground blocks exist.
		//when a background block exists but not a foreground block, the light level is half the computed value.
		int rows = world.getBlocks().getHeight();
		int columns = world.getBlocks().getWidth();

		for(int column = 0; column < columns; column++) {
			setLighting(column, rows - 1, getSolid(column, rows - 1) ? LIGHT_PENETRATION : 1.0f);
		}

		for(int row = rows - 2; row >= 0; row--) {
			for(int column = 1; column < columns - 1; column++) {
				Block block = world.getBlocks().get(World.LAYER_MAIN, column, row);
				if(block != null && block.isSolid()) {
					setLighting(column, row, getLighting(column, row + 1) * LIGHT_PENETRATION);
				} else {
					float above = getLighting(column, row + 1);
					float aboveLeft = getLighting(column - 1, row + 1);
					float aboveRight = getLighting(column + 1, row + 1);
					setLighting(column, row, (above + aboveLeft + aboveRight)/3f);
				}
			}
		}
	}

	private boolean getSolid(int x, int y) {
		Block block = world.getBlocks().get(World.LAYER_MAIN, x, y);
		return block != null && block.isSolid(); //TODO check opacity somehow
	}

	private void setLighting(int x, int y, float exposure) {
		lighting[world.getBlocks().getWidth() * y + x] = exposure;
	}

	private float getLighting(int x, int y) {
		return lighting[world.getBlocks().getWidth() * y + x];
	}

	public void updateLighting(int x, int y) {
		int layer = world.getBlocks().getTopBlockLayer(x, y);

		switch(layer) {
			case World.LAYER_MAIN -> {

			}

			case World.LAYER_BACKGROUND -> {

			}

			case -1 -> {
				setLighting(x, y, 1.0f);
			}

			default -> {

			}
		}
	}

	public void render(Framebuffer framebuffer, final int width, final int height, float daylight) {
		//ensure that model program, camera, world are cached on stack
		ModelRenderer program = this.modelProgram;
		Camera camera = this.camera;
		World world = this.world;

		//set the current shader program
		program.setCurrent();
		framebuffer.setDraw();

		program.loadViewMatrixStandard(width, height);

		//render before loading view matrix
		float scale = 2f * (width > height ? width / (float)height : height / (float)width);
		program.queueRender(GameModels.MODEL_SKY, 1.0f/daylight, daylight, 0, 0, scale, scale, 0);
		program.queueRender(GameModels.MODEL_NIGHT_SKY, 1 - daylight, 1.0f, 0, 0, scale, scale, 0);
		//load the view transformation
		program.loadViewMatrix(camera, width, height);

		//get visible world coordinates
		float worldLeft = ClientUtility.getViewLeftBound(camera, width, height);
		float worldRight = ClientUtility.getViewRightBound(camera, width, height);
		float worldTop = ClientUtility.getViewTopBound(camera, width, height);
		float worldBottom = ClientUtility.getViewBottomBound(camera, width, height);

		//cache foreground and background of world
		BlockGrid blocks = world.getBlocks();

		int topIndex = blocks.getHeight() - 1;
		int rightIndex = blocks.getWidth() - 1;

		//TODO these could cause errors if the camera is far enough past the edge of the world
		//get visible block grid bounds
		int leftBound = Math.max(0, (int)worldLeft);
		int bottomBound = Math.max(0, (int)worldBottom);
		int topBound = Math.min(Math.round(worldTop), topIndex);
		int rightBound = Math.min(Math.round(worldRight), rightIndex);

		//render the blocks visible in the viewport
		for(int row = bottomBound; row <= topBound; row++) {
			for(int column = leftBound; column <= rightBound; column++) {
				var back = (ClientBlockProperties)blocks.get(World.LAYER_BACKGROUND, column, row);
				var front = (ClientBlockProperties)blocks.get(World.LAYER_MAIN, column, row);
				float exposure = getLighting(column, row) * daylight;

				if(back != null && (front == null || front.isTransparent())) {
					program.queueRender(back.getModel(), 1.0f, exposure * LAYER_EXPOSURE_FACTOR, column, row, 1.0f, 1.0f, 0.0f);
				}

				if(front != null) {
					program.queueRender(front.getModel(), 1.0f, exposure, column, row, 1.0f, 1.0f, 0.0f);
				}
			}
		}

		//render the entities
		for(Entity e : world) {
			//pre-compute variables
			float posX = e.getPositionX();
			float posY = e.getPositionY();
			float halfWidth = e.getWidth() / 2;
			float halfHeight = e.getHeight() / 2;

			//check if the entity is visible inside the viewport and render it
			if(posX < worldRight + halfWidth && posX > worldLeft - halfWidth && posY < worldTop + halfHeight && posY > worldBottom - halfHeight) {
				((Renderable)e).render(program, computeExposure(posX, posY, topIndex, rightIndex) * daylight);
			}
		}
	}

	private float computeExposure(float posX, float posY, int topIndex, int rightIndex) {
		float ratioX = posX % 1;
		float ratioY = posY % 1;
		int left = Math.max(0, (int)Math.floor(posX));
		int right = Math.min((int)Math.ceil(posX), rightIndex);
		int bottom = Math.max(0, (int)Math.floor(posY));
		int top = Math.min((int)Math.ceil(posY), topIndex);

		//TODO deal with what happens when entity is outside of the world in some or all dimensions (currently causes array index OOB)
		float exposureBottom = lerp(
			getLighting(left, bottom),
			getLighting(right, bottom),
			ratioX
		);

		float exposureTop = lerp(
			getLighting(left, top),
			getLighting(right, top),
			ratioX
		);

		return lerp(
			exposureBottom,
			exposureTop,
			ratioY
		);
	}

	private static float lerp(float a, float b, float f) {
		return Math.fma(f, (b - a), a);
	}
}