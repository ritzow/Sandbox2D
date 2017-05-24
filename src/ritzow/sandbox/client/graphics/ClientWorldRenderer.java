package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import ritzow.sandbox.client.ClientWorld;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.component.Luminous;
import ritzow.sandbox.world.entity.Entity;

public final class ClientWorldRenderer implements Renderer {
	private final ClientWorld world;
	private final ModelRenderProgram modelProgram;
	private final LightRenderProgram lightProgram;
	private final Framebuffer framebuffer, secondFramebuffer;
	private final Texture diffuseTexture;
	private final Texture shadowTexture;
	private int previousWidth, previousHeight;
	
	public ClientWorldRenderer(ModelRenderProgram modelProgram, LightRenderProgram lightProgram, ClientWorld world) {
		this.world = world;
		this.modelProgram = modelProgram;
		this.lightProgram = lightProgram;
		this.framebuffer = new Framebuffer();
		this.secondFramebuffer = new Framebuffer();
		this.diffuseTexture = new Texture(100, 100);
		this.shadowTexture = new Texture(100, 100);
		framebuffer.attachTexture(diffuseTexture, GL_COLOR_ATTACHMENT0);
		secondFramebuffer.attachTexture(shadowTexture, GL_COLOR_ATTACHMENT0);
		GraphicsUtility.checkErrors();
	}
	
	@Override
	public Framebuffer render(int framebufferWidth, int framebufferHeight) {
		
		//ensure that model program is cached on stack
		ModelRenderProgram modelProgram = this.modelProgram;
		
		//update framebuffer size
		if(previousWidth != framebufferWidth || previousHeight != framebufferHeight) {
			modelProgram.setResolution(framebufferWidth, framebufferHeight);
			diffuseTexture.setSize(framebufferWidth, framebufferHeight);
			shadowTexture.setSize(framebufferWidth, framebufferHeight);
			previousWidth = framebufferWidth;
			previousHeight = framebufferHeight;
		}
		
		//set the current shader program
		modelProgram.setCurrent();
		
		//load the view transformation
		modelProgram.loadViewMatrix(true);
		
		//get visible world coordinates
		float worldLeft = modelProgram.getWorldViewportLeftBound();
		float worldRight = modelProgram.getWorldViewportRightBound();
		float worldTop = modelProgram.getWorldViewportTopBound();
		float worldBottom = modelProgram.getWorldViewportBottomBound();
		
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
		
		//render the blocks visible in the viewport
		for(int row = bottomBound; row <= topBound; row++) {
			for(int column = leftBound; column <= rightBound; column++) {
				if(foreground.isBlock(column, row)) {
					modelProgram.render(Models.forIndex(foreground.get(column, row).getModelIndex()), 1.0f, column, row, 1.0f, 1.0f, 0.0f);
				} else if(background.isBlock(column, row)) {
					modelProgram.render(Models.forIndex(background.get(column, row).getModelIndex()), 0.5f, column, row, 1.0f, 1.0f, 0.0f);
				}
			}
		}
		
		//render the entities
		for(Entity e : world) {
			if(e != null) {
				//pre-compute variables
				float posX = e.getPositionX();
				float posY = e.getPositionY();
				float halfWidth = e.getWidth()/2;
				float halfHeight = e.getHeight()/2;
				
				//check if the entity is visible inside the viewport and render it
				if(posX < worldRight + halfWidth && posX > worldLeft - halfWidth && posY < worldTop + halfHeight && posY > worldBottom - halfHeight) {
					e.render(modelProgram); //TODO create special object to pass to render method that restricts direct program access but allows entities to customize their rendering
				}
			}
		}
		
		/* Need to "erase" blackness of shadowTexture where there is lighting.
		 * render to shadow texture, sample from diffuse, blend diffuse and light value with shadow texture
		 * of pixel to determine shadow texture pixel color, but remember that there can be more than 1 light 
		 * and they need to blend well, no complete overwriting of values
		 * http://www.soolstyle.com/2010/02/15/2d-deferred-lightning/
		 */
		
		lightProgram.setCurrent();
		framebuffer.attachTexture(diffuseTexture, GL_COLOR_ATTACHMENT0);
		framebuffer.setDraw();
		
		for(Entity e : world) {
			if(e != null && e instanceof Luminous) {
				//pre-compute variables
				Luminous light = (Luminous)e;
				float posX = e.getPositionX();
				float posY = e.getPositionY();
				float halfWidth = light.getLightRadius();
				float halfHeight = halfWidth;
				
				//check if the entity is visible inside the viewport and render it
				if(posX < worldRight + halfWidth && posX > worldLeft - halfWidth && posY < worldTop + halfHeight && posY > worldBottom - halfHeight) {
					//lightProgram.render(light, posX, posY, framebufferWidth, framebufferHeight);
				}
			}
		}
	    return framebuffer;
	}
}