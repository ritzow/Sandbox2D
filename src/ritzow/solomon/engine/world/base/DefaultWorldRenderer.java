package ritzow.solomon.engine.world.base;

import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import org.lwjgl.opengl.GL20;
import ritzow.solomon.engine.graphics.Framebuffer;
import ritzow.solomon.engine.graphics.GraphicsUtility;
import ritzow.solomon.engine.graphics.Models;
import ritzow.solomon.engine.graphics.Renderer;
import ritzow.solomon.engine.graphics.Texture;
import ritzow.solomon.engine.world.component.Luminous;
import ritzow.solomon.engine.world.entity.Entity;

public final class DefaultWorldRenderer implements Renderer {
	private final DefaultWorld world;
	private final ModelRenderProgram modelProgram;
	private final LightRenderProgram lightProgram;
	
	private final Framebuffer framebuffer;
	private final Texture diffuseTexture;
	private int previousWidth, previousHeight;
	
	public DefaultWorldRenderer(ModelRenderProgram modelProgram, LightRenderProgram lightProgram, DefaultWorld world) {
		this.world = world;
		this.modelProgram = modelProgram;
		this.lightProgram = lightProgram;
		this.framebuffer = new Framebuffer();
		this.diffuseTexture = new Texture(100, 100);
		GraphicsUtility.checkErrors();
	}
	
	@Override
	public Framebuffer render(int framebufferWidth, int framebufferHeight) {
		//set the current shader program
		modelProgram.setCurrent();
		
		//update framebuffer size
		if(previousWidth != framebufferWidth || previousHeight != framebufferHeight) {
			modelProgram.setResolution(framebufferWidth, framebufferHeight);
			diffuseTexture.recreate(framebufferWidth, framebufferHeight);
			previousWidth = framebufferWidth;
			previousHeight = framebufferHeight;
		}
		
		//load the view transformation
		modelProgram.loadViewMatrix(true);
		
		//get visible world coordinates
		float worldLeft = modelProgram.getWorldViewportLeftBound();
		float worldRight = modelProgram.getWorldViewportRightBound();
		float worldTop = modelProgram.getWorldViewportTopBound();
		float worldBottom = modelProgram.getWorldViewportBottomBound();
		
		//calculate block grid bounds
		int leftBound = 	Math.max(0, (int)Math.floor(worldLeft));
		int rightBound = 	Math.min(world.foreground.getWidth(), (int)Math.ceil(worldRight));
		int topBound = 		Math.min(world.foreground.getHeight(), (int)Math.ceil(worldTop));
		int bottomBound = 	Math.max(0, (int)Math.floor(worldBottom));
		
		//prepare the diffuse texture for drawing
		framebuffer.attachTexture(diffuseTexture, GL_COLOR_ATTACHMENT0);
		framebuffer.clear(1.0f, 1.0f, 1.0f, 1.0f);
		framebuffer.setDraw();
		
		GL20.glDrawBuffers(GL_COLOR_ATTACHMENT0);
		
		//render the blocks visible in the viewport
		for(int row = bottomBound; row <= topBound; row++) {
			for(int column = leftBound; column <= rightBound; column++) {
				if(world.foreground.isBlock(column, row)) {
					modelProgram.render(Models.forIndex(world.foreground.get(column, row).getModelIndex()), 1.0f, column, row, 1.0f, 1.0f, 0.0f);
				} else if(world.background.isBlock(column, row)) {
					modelProgram.render(Models.forIndex(world.background.get(column, row).getModelIndex()), 0.5f, column, row, 1.0f, 1.0f, 0.0f);
				}
			}
		}
		
		//render the entities
		for(int i = 0; i < world.entities.size(); i++) {
			Entity e = world.entities.get(i);
			
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
		
		/*
		 * TODO make shader program that does nothing in vertex shader but processes 
		 * a single light in the fragment shader, call it with diffuse texture/fbo bound? 
		 * run for each light in the scene. do lighting falloff in fragment shader.  
		 * perhaps use occuluder/shadowmap texture sampling to check if a spot should be lit?
		 */
		
		//switch to the light renderer, but continue using world drawing framebuffer
		lightProgram.setCurrent();
		
		//setup shadow map texture for use by a sampler in the fragment shader to esnure that lights dont go through walls
		//glActiveTexture(GL_TEXTURE0);
		
		for(int i = 0; i < world.entities.size(); i++) {
			Entity e = world.entities.get(i);
			if(e instanceof Luminous) {
				lightProgram.render((Luminous)e, e.getPositionX(), e.getPositionY());
			}
		}
		
		//TODO render luminous blocks
		
	    return framebuffer;
	}
}