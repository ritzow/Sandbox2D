package ritzow.sandbox.client.world;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.BufferUtils;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.client.world.block.ClientBlockProperties;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.entity.Entity;

//TODO look into terrraria lighting
public final class ClientWorldRendererLightmap {
	private static final float LIGHT_PENETRATION = 0.75f;
	public static final float BACKGROUND_EXPOSURE_RATIO = 0.5f;

	private final World world;
	private final Camera camera;
	private final List<Lit> lights;
	private final OpenGLByteTexture solidMap, shadingMap;
	private final OpenGLTexture lightOverlay;
	private final Framebuffer shadingFramebuffer;

	public ClientWorldRendererLightmap(Display display, Camera camera, World world) {
		this.camera = camera;
		this.world = world;
		this.lights = new ArrayList<>();
		this.solidMap = new OpenGLByteTexture(buildSolidMap(world.getBlocks()), world.getBlocks().getWidth(), world.getBlocks().getHeight());
		this.shadingMap = new OpenGLByteTexture(world.getBlocks().getWidth(), world.getBlocks().getHeight());
		this.lightOverlay = new OpenGLTexture(display.width(), display.height());
		this.shadingFramebuffer = new Framebuffer();
		shadingFramebuffer.attachTexture(shadingMap, 0);
		GraphicsUtility.checkErrors();
	}

	private ByteBuffer buildSolidMap(BlockGrid blocks) {
		ByteBuffer buffer = BufferUtils.createByteBuffer(blocks.getWidth() * blocks.getHeight());
		for(int row = 0; row < blocks.getHeight(); row++) {
			for(int column =  0; column < blocks.getWidth(); column++) {
				buffer.put(light(column, row));
			}
		}

		return buffer.flip();
	}

	public void updateFramebuffers(int width, int height) {
		lightOverlay.setSize(width, height);
	}

	public void addLight(Lit entity) {
		lights.add(entity);
	}

	public void removeLight(Lit entity) {
		lights.remove(entity);
	}

	//TODO needs to know more information
	public void updateLighting(int x, int y) {
		solidMap.setPixel(x, y, light(x, y));
	}

	private byte light(int x, int y) {
		int layer = world.getBlocks().getTopBlockLayer(x, y);
		return switch(layer) {
			case BlockGrid.INVALID_LAYER, World.LAYER_BACKGROUND -> (byte)255;
			default -> (byte)0;
		};
	}

	public void render(Framebuffer target, final int width, final int height, float daylight) {
		//get visible world coordinates
		float worldLeft = ClientUtility.getViewLeftBound(camera, width, height);
		float worldRight = ClientUtility.getViewRightBound(camera, width, height);
		float worldTop = ClientUtility.getViewTopBound(camera, width, height);
		float worldBottom = ClientUtility.getViewBottomBound(camera, width, height);

		//set the current shader program
		RenderManager.setViewport(width, height);
		RenderManager.setStandardBlending();
		RenderManager.MODEL_RENDERER.prepare();

		RenderManager.MODEL_RENDERER.loadViewMatrixStandard(width, height);

		target.setCurrent();
		float scale = 2f * (width > height ? width / (float)height : height / (float)width);
		RenderManager.MODEL_RENDERER.queueRender(GameModels.MODEL_SKY, 1.0f/daylight, daylight, 0, 0, scale, scale, 0);
		RenderManager.MODEL_RENDERER.queueRender(GameModels.MODEL_NIGHT_SKY, 1 - daylight, 1.0f, 0, 0, scale, scale, 0);
		RenderManager.MODEL_RENDERER.flush();

		if(worldLeft < ClientUtility.getBlocksRight(world.getBlocks()) &&
			worldRight > ClientUtility.getBlocksLeft() &&
			worldBottom < ClientUtility.getBlocksTop(world.getBlocks()) && worldTop > ClientUtility.getBlocksBottom()) {
			//cache foreground and background of world
			BlockGrid blocks = world.getBlocks();

			//TODO these could cause errors if the camera is far enough past the edge of the world
			//TODO could possibly just put bounds check using above values in if statement and not render blocks at all in that case
			//get visible block grid bounds
			int leftBound = Utility.clamp(0, (int)worldLeft, blocks.getWidth() - 1); //Math.max(0, (int)worldLeft);
			int bottomBound = Utility.clamp(0, (int)worldBottom, blocks.getHeight() - 1);//Math.max(0, (int)worldBottom);
			int rightBound = Utility.clamp(0, Math.round(worldRight), blocks.getWidth() - 1); //Math.min(Math.round(worldRight), blocks.getWidth() - 1);
			int topBound = Utility.clamp(0, Math.round(worldTop), blocks.getHeight() - 1);

			RenderManager.SHADING_PROGRAM.render(solidMap, shadingMap, leftBound, bottomBound, rightBound, topBound);

			renderBlocks(
				blocks,
				leftBound,
				bottomBound,
				rightBound,
				topBound,
				width,
				height,
				daylight
			);
		}

		renderEntities(
			worldLeft,
			worldBottom,
			worldRight,
			worldTop,
			width,
			height,
			daylight
		);

		//for now render to final output
		RenderManager.SHADING_APPLY_PROGRAM.render(shadingMap, camera, width, height);

		//TODO first: block lighting calculation
		//pre-render: make sure world-sized (one block per pixel) texture is up-to-date with all proper light values at each block.
		//step series 1: calculating sunlight
		//block final lighting will be calculated by taking an average of a block's surrounding light levels in a certain radius (fixed radius for sunlight penetration)
		//this can be done on GPU per frame if light levels can be stored in texture then lighting computed in fragment shader for viewport region (using proper coordinates)
		//world-sized texture with proper per-block (per-pixel) light levels will then be sampled in a fullscreen fragment shader and blurred
		//step series 2:
		//the process will be repeated potentially using some of the same textures but with foreground (light-blocking) blocks only.
		//then point lights will be applied to this fullscreen blurred block lighting texture that has proper dissipation within blocks (created by the blur function)
		//the sunlight and point light fullscreen textures and fully-lit diffuse texture will be combined in a fragment shader to produce the properly lit scene

		//Alternative for point lights: integrate them within the same block lighting system as 'flood' lights instead, would need to be computed on CPU
		//since is a scattering operation intead of a gathering one.
	}

	private void renderBlocks(BlockGrid blocks, int leftBound, int bottomBound, int rightBound, int topBound, int width, int height, float daylight) {
		RenderManager.BLOCK_RENDERER.prepare();
		RenderManager.BLOCK_RENDERER.setView(camera, width, height);
		RenderManager.BLOCK_RENDERER.setBounds(leftBound, bottomBound, rightBound - leftBound + 1);
		RenderManager.BLOCK_RENDERER.setExposure(daylight * BACKGROUND_EXPOSURE_RATIO);

		for(int row = bottomBound; row <= topBound; row++) {
			for(int column = leftBound; column <= rightBound; column++) {
				var back = (ClientBlockProperties)blocks.get(World.LAYER_BACKGROUND, column, row);
				var front = (ClientBlockProperties)blocks.get(World.LAYER_MAIN, column, row);

				if(back != null && (front == null || front.isTransparent())) {
					RenderManager.BLOCK_RENDERER.queue(back);
				} else {
					RenderManager.BLOCK_RENDERER.skip(1);
				}
			}
		}

		RenderManager.BLOCK_RENDERER.finish();

		RenderManager.BLOCK_RENDERER.setExposure(daylight);

		for(int row = bottomBound; row <= topBound; row++) {
			for(int column = leftBound; column <= rightBound; column++) {
				var front = (ClientBlockProperties)blocks.get(World.LAYER_MAIN, column, row);

				if(front != null) {
					RenderManager.BLOCK_RENDERER.queue(front);
				} else {
					RenderManager.BLOCK_RENDERER.skip(1);
				}
			}
		}

		RenderManager.BLOCK_RENDERER.finish();
	}

	private void renderEntities(float worldLeft, float worldBottom, float worldRight, float worldTop, int width, int height, float daylight) {
		RenderManager.MODEL_RENDERER.prepare();

		//load the view transformation
		RenderManager.MODEL_RENDERER.loadViewMatrix(camera, width, height);

		//render the entities
		for(Entity e : world) {
			//pre-compute variables
			float posX = e.getPositionX();
			float posY = e.getPositionY();
			float halfWidth = e.getWidth() / 2;
			float halfHeight = e.getHeight() / 2;

			//check if the entity is visible inside the viewport and render it
			if(posX < worldRight + halfWidth && posX > worldLeft - halfWidth && posY < worldTop + halfHeight && posY > worldBottom - halfHeight) {
				((Renderable)e).render(RenderManager.MODEL_RENDERER, daylight);
			}
		}

		RenderManager.MODEL_RENDERER.flush();
	}
}