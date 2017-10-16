package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT1;

import java.util.Collection;
import ritzow.sandbox.client.input.controller.Controller;
import ritzow.sandbox.client.util.Renderable;
import ritzow.sandbox.client.world.block.ClientBlock;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;

public final class ClientGameRenderer implements Renderer {
	private static final float MAX_TIMESTEP = 2;
	
	private final ModelRenderProgram modelProgram;
//	private final LightRenderProgram lightProgram;
	private final Framebuffer framebuffer;
	private final Texture diffuseTexture;
	private final Texture finalTexture;
	private int previousWidth, previousHeight;
	
	private final World world;
	private final Collection<Controller> controllers;
	private long previousTime;
	
	public ClientGameRenderer(Collection<Controller> controllers, ModelRenderProgram modelProgram, LightRenderProgram lightProgram, World world) {
		this.world = world;
		this.controllers = controllers;
		this.modelProgram = modelProgram;
//		this.lightProgram = lightProgram;
		this.framebuffer = new Framebuffer();
		this.diffuseTexture = new Texture(100, 100);
		this.finalTexture = new Texture(100, 100);
		framebuffer.attachTexture(diffuseTexture, GL_COLOR_ATTACHMENT0);
		framebuffer.attachTexture(finalTexture, GL_COLOR_ATTACHMENT1);
		GraphicsUtility.checkErrors();
		this.previousTime = System.nanoTime();
	}
	
	@Override
	public Framebuffer render(final int currentWidth, final int currentHeight) {
		//update the world (this section might eventually be moved to its own "renderer" that runs before the clientworldrenderer)
		//TODO this only really works for client side stuff doesnt it, if connected to a server it will act weird/be redundant
		long current = System.nanoTime(); //get the current time
		float totalUpdateTime = (current - previousTime) * 0.0000000625f; //get the amount of update time
		previousTime = current; //update the previous time for the next frame
		
		//update the world with a timestep of at most 2 until the world is up to date.
		for(float time; totalUpdateTime > 0; totalUpdateTime -= time) {
			time = Math.min(totalUpdateTime, MAX_TIMESTEP);
			world.update(time);
			totalUpdateTime -= time;
		}
		
		//update controllers (camera, player, etc.)
		controllers.forEach(c -> {
			c.update();
		});
		
		//ensure that model program is cached on stack
		ModelRenderProgram modelProgram = this.modelProgram;
		
		//update framebuffer size
		if(previousWidth != currentWidth || previousHeight != currentHeight) {
			modelProgram.setResolution(currentWidth, currentHeight);
			diffuseTexture.setSize(currentWidth, currentHeight);
			finalTexture.setSize(currentWidth, currentHeight);
			previousWidth = currentWidth; previousHeight = currentHeight;
		}
		
		//set the current shader program
		modelProgram.setCurrent();
		
		//load the view transformation
		modelProgram.loadViewMatrix(true);
		
		//get visible world coordinates
		final float worldLeft = modelProgram.getWorldViewportLeftBound(),
					worldRight = modelProgram.getWorldViewportRightBound(),
					worldTop = modelProgram.getWorldViewportTopBound(),
					worldBottom = modelProgram.getWorldViewportBottomBound();
		
		//cache foreground and background of world
		final BlockGrid foreground = world.getForeground(), background = world.getBackground();
		
		//calculate block grid bounds
		int leftBound = 	Math.max(0, (int)Math.floor(worldLeft));
		int rightBound = 	Math.min(foreground.getWidth(), (int)Math.ceil(worldRight));
		int topBound = 		Math.min(foreground.getHeight(), (int)Math.ceil(worldTop));
		int bottomBound = 	Math.max(0, (int)Math.floor(worldBottom));
		
		//prepare the diffuse texture for drawing
		framebuffer.clear(1.0f, 1.0f, 1.0f, 1.0f);
		framebuffer.setDraw();
		
		//tell the framebuffer to draw the shader output to attachment 0
		glDrawBuffers(GL_COLOR_ATTACHMENT0);
		
		//set the blending mode to allow transparency
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		//render the blocks visible in the viewport
		for(int row = bottomBound; row <= topBound; row++) {
			for(int column = leftBound; column <= rightBound; column++) {
				if(foreground.isBlock(column, row)) {
					ClientBlock block = (ClientBlock)foreground.get(column, row);
					modelProgram.render(Models.forIndex(block.getModelIndex()), 1.0f, column, row, 1.0f, 1.0f, 0.0f);
				} else if(background != null && background.isBlock(column, row)) { //TODO temp null check
					ClientBlock block = (ClientBlock)background.get(column, row);
					modelProgram.render(Models.forIndex(block.getModelIndex()), 0.5f, column, row, 1.0f, 1.0f, 0.0f); 
					//TODO when the player destroys a block (happens on a different thread), this can cause a null pointer
				}
			}
		}
		
		//render the entities
		world.forEach(e -> {
			//pre-compute variables
			float posX = e.getPositionX();
			float posY = e.getPositionY();
			float halfWidth = e.getWidth()/2;
			float halfHeight = e.getHeight()/2;
			
			//check if the entity is visible inside the viewport and render it
			if(posX < worldRight + halfWidth && posX > worldLeft - halfWidth && posY < worldTop + halfHeight && posY > worldBottom - halfHeight) {
				((Renderable)e).render(modelProgram);
			}
		});
		
		/* Need to "erase" blackness of shadowTexture where there is lighting.
		 * render to shadow texture, sample from diffuse, blend diffuse and light value with shadow texture
		 * of pixel to determine shadow texture pixel color, but remember that there can be more than 1 light 
		 * and they need to blend well, no complete overwriting of values
		 * http://www.soolstyle.com/2010/02/15/2d-deferred-lightning/
		 */
		
//		lightProgram.setCurrent();
//		glDrawBuffers(GL_COLOR_ATTACHMENT1);
//		framebuffer.clear(0, 0, 0, 1);
//		glClear(GL_COLOR_BUFFER_BIT);
//		glBlendFunc(GL_SRC_ALPHA, GL_ZERO);
//		world.forEach(e -> {
//			if(e instanceof Luminous) {
//				//pre-compute variables
//				Luminous light = (Luminous)e;
//				float posX = e.getPositionX();
//				float posY = e.getPositionY();
//				float halfWidth = light.getLightRadius();
//				float halfHeight = halfWidth;
//				
//				//check if the entity is visible inside the viewport and render it
//				if(posX < worldRight + halfWidth && posX > worldLeft - halfWidth && posY < worldTop + halfHeight && posY > worldBottom - halfHeight) {
//					lightProgram.render(light, posX, posY, currentWidth, currentHeight);
//				}
//			}
//		});
//		
//		glDrawBuffers(GL_COLOR_ATTACHMENT0);
	    return framebuffer;
	}
}