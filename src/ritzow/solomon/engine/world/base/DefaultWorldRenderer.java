package ritzow.solomon.engine.world.base;

import ritzow.solomon.engine.graphics.Camera;
import ritzow.solomon.engine.graphics.Models;
import ritzow.solomon.engine.graphics.Renderer;
import ritzow.solomon.engine.world.entity.Entity;

public final class DefaultWorldRenderer implements Renderer { //TODO create some framebuffers for storing shadow/lighting information
	private final DefaultWorld world;
	private final ModelRenderProgram program;
	
	public DefaultWorldRenderer(ModelRenderProgram program, Camera camera, DefaultWorld world) {
		this.world = world;
		this.program = program;
	}
	
	@Override
	public void render() {
		program.setCurrent();
		program.loadViewMatrix(true);
		
		float worldLeft = program.getWorldViewportLeftBound();
		float worldRight = program.getWorldViewportRightBound();
		float worldTop = program.getWorldViewportTopBound();
		float worldBottom = program.getWorldViewportBottomBound();
		
		//calculate block grid bounds
		int leftBound = 	Math.max(0, (int)Math.floor(worldLeft));
		int rightBound = 	Math.min(world.foreground.getWidth(), (int)Math.ceil(worldRight));
		int topBound = 		Math.min(world.foreground.getHeight(), (int)Math.ceil(worldTop));
		int bottomBound = 	Math.max(0, (int)Math.floor(worldBottom));
		
		//render the blocks visible in the viewport
		for(int row = bottomBound; row <= topBound; row++) {
			for(int column = leftBound; column <= rightBound; column++) {
				if(world.foreground.isBlock(column, row)) {
					program.render(Models.forIndex(world.foreground.get(column, row).getModelIndex()), 1.0f, column, row, 1.0f, 1.0f, 0.0f);
				} else if(world.background.isBlock(column, row)) {
					program.render(Models.forIndex(world.background.get(column, row).getModelIndex()), 0.5f, column, row, 1.0f, 1.0f, 0.0f);
				}
			}
		}
		
		//render the entities
		for(int i = 0; i < world.entities.size(); i++) {
			Entity e = world.entities.get(i);
			
			if(e != null) {
				float posX = e.getPositionX();
				float posY = e.getPositionY();
				float halfWidth = e.getWidth()/2;
				float halfHeight = e.getHeight()/2;
				
				//check if the entity is visible inside the viewport and render it
				if(posX < worldRight + halfWidth && posX > worldLeft - halfWidth && posY < worldTop + halfHeight && posY > worldBottom - halfHeight) {
					e.render(program);
				}
			}
		}
	}
	
	@Override
	public void updateSize(int width, int height) {
		program.setResolution(width, height);
	}
}