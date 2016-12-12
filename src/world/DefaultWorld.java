package world;

import graphics.Model;
import graphics.ModelRenderer;
import world.entity.Entity;

public class DefaultWorld extends AbstractWorld {

	public DefaultWorld(int width, int height) {
		super(width, height);
	}

	@Override
	public void render(ModelRenderer renderer) {
		renderer.loadViewMatrix(true);
		
		int leftBound = 	Math.max(0, (int)Math.floor(renderer.getWorldViewportLeftBound()));
		int bottomBound = 	Math.max(0, (int)Math.floor(renderer.getWorldViewportBottomBound()));
		int rightBound = 	(int)Math.min(getWidth(), Math.ceil(renderer.getWorldViewportRightBound()) + 1);
		int topBound = 		(int)Math.min(getHeight(), Math.ceil(renderer.getWorldViewportTopBound()) + 1);
		
		for(int row = bottomBound; row < topBound; row++) {
			for(int column = leftBound; column < rightBound; column++) {
				if(getBlockForeground(column, row) != null) {
					Model blockModel = getBlockForeground(column, row).getModel();
					
					if(blockModel != null) {
						renderer.loadOpacity(1);
						renderer.loadTransformationMatrix(column, row, 1, 1, 0);
						blockModel.render();
					}
				}
				
				else if(getBlockBackground(column, row) != null) {
					Model blockModel = getBlockBackground(column, row).getModel();
					
					if(blockModel != null) {
						renderer.loadOpacity(0.5f); //TODO this is a temp representation of background blocks, in actuality, they will be opaque but draw over.
						renderer.loadTransformationMatrix(column, row, 1, 1, 0);
						blockModel.render();
					}
				}
			}
		}
		
		for(Entity e : entities) {
			if(e == null)
				continue;
			
			if(e.getPositionX() < renderer.getWorldViewportRightBound() + e.getWidth()/2 
				&& e.getPositionX() > renderer.getWorldViewportLeftBound() - e.getWidth()/2 
				&& e.getPositionY() < renderer.getWorldViewportTopBound() + e.getHeight()/2 
				&& e.getPositionY() > renderer.getWorldViewportBottomBound() - e.getHeight()/2)
						e.render(renderer);
		}
	}

	@Override
	public void update(float time) {
		
	}

}
