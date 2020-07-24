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

	private static final float LIGHT_PENETRATION = 0.75f;
	private static final int SKYLIGHT_RADIUS = 10;

	private void initLighting() {
		int rows = world.getBlocks().getHeight();
		int columns = world.getBlocks().getWidth();

		for(int column = 0; column < columns; column++) {
			setLighting(column, rows - 1, getSolid(column, rows - 1) ? LIGHT_PENETRATION : 1.0f);
		}

		for(int row = 0; row < rows; row++) {
			setLighting(0, row, 1.0f);
			setLighting(columns - 1, row, 1.0f);
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

	public void render(Framebuffer framebuffer, final int width, final int height) {
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
		program.queueRender(GameModels.MODEL_SKY, 1.0f, 0, 0, scale, scale, 0);
		//load the view transformation
		program.loadViewMatrix(camera, width, height);

		//get visible world coordinates
		float worldLeft = ClientUtility.getViewLeftBound(camera, width, height);
		float worldRight = ClientUtility.getViewRightBound(camera, width, height);
		float worldTop = ClientUtility.getViewTopBound(camera, width, height);
		float worldBottom = ClientUtility.getViewBottomBound(camera, width, height);

		//cache foreground and background of world
		BlockGrid blocks = world.getBlocks();

		//get visible block grid bounds
		int leftBound = Math.max(0, (int)worldLeft);
		int bottomBound = Math.max(0, (int)worldBottom);
		int topBound = Math.min(Math.round(worldTop), blocks.getHeight() - 1);
		int rightBound = Math.min(Math.round(worldRight), blocks.getWidth() - 1);

		//render the blocks visible in the viewport
		for(int row = bottomBound; row <= topBound; row++) {
			for(int column = leftBound; column <= rightBound; column++) {
				var back = (ClientBlockProperties)blocks.get(World.LAYER_BACKGROUND, column, row);
				var front = (ClientBlockProperties)blocks.get(World.LAYER_MAIN, column, row);
				float exposure = getLighting(column, row); //(float)row/blocks.getHeight();

				if(back != null && (front == null || front.isTransparent())) {
					program.queueRender(back.getModel(), 1.0f, exposure/2f, column, row, 1.0f, 1.0f, 0.0f);
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
				((Renderable)e).render(program, computeExposure(posX, posY));
			}
		}
	}

	private float computeExposure(float posX, float posY) {
		float ratioX = posX % 1;
		float ratioY = posY % 1;
		int left = (int)Math.floor(posX);
		int right = (int)Math.ceil(posX);
		int bottom = (int)Math.floor(posY);
		int top = (int)Math.ceil(posY);
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