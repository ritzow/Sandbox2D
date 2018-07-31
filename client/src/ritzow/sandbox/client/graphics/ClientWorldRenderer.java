package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL30C.*;

import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.client.world.block.ClientBlockProperties;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;

public final class ClientWorldRenderer implements Renderer {
	private final ModelRenderProgram modelProgram;
	private final Framebuffer framebuffer;
	private final OpenGLTexture diffuseTexture;
	private int previousWidth, previousHeight;
	private final World world;
	private final Camera camera;

	public ClientWorldRenderer(ModelRenderProgram modelProgram, Camera camera, World world) {
		this.modelProgram = modelProgram;
		this.camera = camera;
		this.world = world;
		this.framebuffer = new Framebuffer();
		this.diffuseTexture = new OpenGLTexture(100, 100);
		framebuffer.attachTexture(diffuseTexture, GL_COLOR_ATTACHMENT0);
		GraphicsUtility.checkErrors();
	}

	@Override
	public Framebuffer render(final int currentWidth, final int currentHeight) {
		//ensure that model program is cached on stack
		ModelRenderProgram modelProgram = this.modelProgram;

		//update framebuffer size
		if(previousWidth != currentWidth || previousHeight != currentHeight) {
			modelProgram.setResolution(currentWidth, currentHeight);
			diffuseTexture.setSize(currentWidth, currentHeight);
			//finalTexture.setSize(currentWidth, currentHeight);
			previousWidth = currentWidth; previousHeight = currentHeight;
		}

		//cache on thread
		Camera camera = this.camera;

		//set the current shader program
		modelProgram.setCurrent();

		//load the view transformation
		modelProgram.loadViewMatrix(camera);

		//get visible world coordinates
		int width = modelProgram.getFrameBufferWidth(), height = modelProgram.getFrameBufferHeight();
		final float worldLeft = ClientUtility.getViewLeftBound(camera, width, height),
					worldRight = ClientUtility.getViewRightBound(camera, width, height),
					worldTop = ClientUtility.getViewTopBound(camera, width, height),
					worldBottom = ClientUtility.getViewBottomBound(camera, width, height);

		//cache foreground and background of world
		final BlockGrid foreground = world.getForeground(), background = world.getBackground();

		//prepare the diffuse texture for drawing
		framebuffer.clear(1.0f, 1.0f, 1.0f, 1.0f);
		framebuffer.setDraw();

		//tell the framebuffer to draw the shader output to attachment 0
		glDrawBuffers(GL_COLOR_ATTACHMENT0);

		//set the blending mode to allow transparency
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		//calculate block grid bounds TODO fix after adding chunk system, allow for negatives
		int leftBound = 	Utility.clampLowerBound(0, worldLeft);
		int rightBound = 	Utility.clampUpperBound(foreground.getWidth()-1, worldRight);
		int topBound = 		Utility.clampUpperBound(foreground.getHeight()-1, worldTop);
		int bottomBound =	Utility.clampLowerBound(0, worldBottom);

		//render the blocks visible in the viewport
		for(int row = bottomBound; row <= topBound; row++) {
			for(int column = leftBound; column <= rightBound; column++) {
				ClientBlockProperties back = (ClientBlockProperties)background.get(column, row),
						front = (ClientBlockProperties)foreground.get(column, row);

				if(back != null && (front == null || front.isTransparent())) {
					modelProgram.render(back.getModelIndex(), 0.5f, column, row, 1.0f, 1.0f, 0.0f);
				}

				if(front != null) {
					modelProgram.render(front.getModelIndex(), 1.0f, column, row, 1.0f, 1.0f, 0.0f);
				}
			}
		}

		//render the entities
		world.forEach(e -> {
			//pre-compute variables
			Renderable graphics = (Renderable)e;
			float posX = e.getPositionX();
			float posY = e.getPositionY();
			float halfWidth = graphics.getWidth()/2;
			float halfHeight = graphics.getHeight()/2;

			//check if the entity is visible inside the viewport and render it
			if(posX < worldRight + halfWidth && posX > worldLeft - halfWidth && posY < worldTop + halfHeight && posY > worldBottom - halfHeight) {
				graphics.render(modelProgram);
			}
		});
	    return framebuffer;
	}
}