package world;

import static util.HitboxUtil.*;

import graphics.Model;
import graphics.Renderable;
import graphics.ModelRenderer;
import java.util.ArrayList;
import world.entity.Entity;

import java.io.Serializable;

public class World implements Renderable, Serializable {	
	private static final long serialVersionUID = 8941044044393756575L;
	
	protected final ArrayList<Entity> entities;
	protected final BlockGrid foreground;
	protected final BlockGrid background;
	protected float gravity;
	
	public World(int width, int height, float gravity) {
		entities = new ArrayList<Entity>();
		foreground = new BlockGrid(this, width, height);
		background = new BlockGrid(this, width, height);
		this.gravity = gravity;
	}
	
	public BlockGrid getForeground() {
		return foreground;
	}
	
	public BlockGrid getBackground() {
		return background;
	}
	
	public ArrayList<Entity> getEntities() {
		return entities;
	}
	
	public float getGravity() {
		return gravity;
	}

	public void setGravity(float gravity) {
		this.gravity = gravity;
	}

	public void update(float time) {
		for(int i = 0; i < entities.size(); i++) {
			Entity e = entities.get(i);
			
			//remove entities that are below the world
			if(e == null || e.getPositionY() < 0 || e.getShouldDelete()) {
				entities.remove(i);
				i = Math.max(0, i - 1);
				continue;
			}
			
			e.update(time);
			
			e.setVelocityY(e.getVelocityY() - gravity * time);
			
			if(e.getDoCollision()) {
				
				//check for entity vs. entity collisions
				for(int j = i + 1; j < entities.size(); j++) {
					Entity o = entities.get(j);
					
					if(o == null)
						continue;
					
					if(o.getDoCollision()) {
						boolean collision;
						
						if(o.getDoEntityCollisionResolution()) {
							collision = (e.getMass() < o.getMass()) ? resolveCollision(e, o, time) : resolveCollision(o, e, time);
						} else {
							collision = checkCollision(e, o);
						}
						
						if(collision) {
							e.onCollision(this, o, time);
							o.onCollision(this, e, time);
						}
					}
				}

				//Check for entity collisions with blocks
				if(e.getDoBlockCollisionResolution()) {
					int leftBound = Math.max(0, (int)Math.floor(e.getPositionX() - e.getWidth()));
					int topBound = Math.min(foreground.getHeight(), (int)Math.ceil(e.getPositionY() + e.getHeight()));
					int rightBound = Math.min(foreground.getWidth(), (int)Math.ceil(e.getPositionX() + e.getWidth()));
					int bottomBound = Math.max(0, (int)Math.floor(e.getPositionY() - e.getHeight()));
					
					for(int row = bottomBound; row < topBound; row++) {
						for(int column = leftBound; column < rightBound; column++) {
							if(foreground.isBlock(column, row) && !foreground.isHidden(column, row)) {
								resolveCollision(e, column, row, 1, 1, foreground.get(column, row).getFriction(), time);
							}
						}
					}
				}
			}
		}
	}
	
	public boolean checkCollision(Entity e, Entity o) {
		return checkCollision(e, o.getPositionX(), o.getPositionY(), o.getWidth(), o.getHeight());
	}
	
	public boolean checkCollision(Entity entity, float otherX, float otherY, float otherWidth, float otherHeight) {
		float width = 0.5f * (entity.getWidth() + otherWidth);
		float height = 0.5f * (entity.getHeight() + otherHeight);
		float deltaX = otherX - entity.getPositionX();
		float deltaY = otherY - entity.getPositionY();
		
		if (Math.abs(deltaX) < width && Math.abs(deltaY) < height) {
			return true;
		}
		
		else {
			return false;
		}
	}
	
	public boolean resolveCollision(Entity e, Entity o, float time) {
		return resolveCollision(e, o.getPositionX(), o.getPositionY(), o.getWidth(), o.getHeight(), o.getFriction(), time);
	}
	
	/** Returns true if a collision occurred **/
	public boolean resolveCollision(Entity entity, float otherX, float otherY, float otherWidth, float otherHeight, float otherFriction, float time) {	
		float width = 0.5f * (entity.getWidth() + otherWidth);
		float height = 0.5f * (entity.getHeight() + otherHeight);
		float deltaX = otherX - entity.getPositionX();
		float deltaY = otherY - entity.getPositionY();
		if (Math.abs(deltaX) < width && Math.abs(deltaY) < height) { /* collision! replace < in intersection detection with <= for previous behavior */
		    float wy = width * deltaY;
		    float hx = height * deltaX;
		    if (wy > hx) {
		        if (wy > -hx) { /* collision on the top of e */
		        	entity.setPositionY(otherY - height);
		        	
					if(entity.getVelocityY() > 0) {
						entity.setVelocityY(0);;
					}
					
		        	if(entity.getVelocityX() > 0) {
		        		entity.setVelocityX(Math.max(0, entity.getVelocityX() - combineFriction(entity.getFriction(), otherFriction) * time));
		        	}
		        	
		        	else if(entity.getVelocityX() < 0) {
		        		entity.setVelocityX(Math.min(0, entity.getVelocityX() + combineFriction(entity.getFriction(), otherFriction) * time));
		        	}
		        }

		        else { /* collision on the left of e */
		        	entity.setPositionX(otherX + width);
					
		        	if(entity.getVelocityX() < 0) {
						entity.setVelocityX(0);
					}
		        	
		        	if(entity.getVelocityY() > 0) {
		        		entity.setVelocityY(Math.max(0, entity.getVelocityY() - combineFriction(entity.getFriction(), otherFriction) * time));
		        	}
		        	
		        	else if(entity.getVelocityY() < 0) {
		        		entity.setVelocityY(Math.min(0, entity.getVelocityY() + combineFriction(entity.getFriction(), otherFriction) * time));
		        	}
		        }
		    }
		    
		    else {
		        if (wy > -hx) { /* collision on the right of e */
		        	entity.setPositionX(otherX - width);
					
		        	if(entity.getVelocityX() > 0) {
						entity.setVelocityX(0);
					}
					
		        	if(entity.getVelocityY() > 0) {
		        		entity.setVelocityY(Math.max(0, entity.getVelocityY() - combineFriction(entity.getFriction(), otherFriction) * time));
		        	}
		        	
		        	else if(entity.getVelocityY() < 0) {
		        		entity.setVelocityY(Math.min(0, entity.getVelocityY() + combineFriction(entity.getFriction(), otherFriction) * time));
		        	}
		        }
		        
		        else { /* collision on the bottom of e */
		        	entity.setPositionY(otherY + height);

		        	if(entity.getVelocityY() < 0) {
		        		entity.setVelocityY(0);
		        	}
		        	
		        	if(entity.getVelocityX() > 0) {
		        		entity.setVelocityX(Math.max(0, entity.getVelocityX() - combineFriction(entity.getFriction(), otherFriction) * time));
		        	}
		        	
		        	else if(entity.getVelocityX() < 0) {
		        		entity.setVelocityX(Math.min(0, entity.getVelocityX() + combineFriction(entity.getFriction(), otherFriction) * time));
		        	}
		        }
		    }
		    
		    return true;
		}
		
		return false;
	}
	
	@Override
	public void render(ModelRenderer renderer) {
		renderer.loadViewMatrix(true);
		
		int leftBound = 	Math.max(0, (int)Math.floor(renderer.getWorldViewportLeftBound()));
		int bottomBound = 	Math.max(0, (int)Math.floor(renderer.getWorldViewportBottomBound()));
		int rightBound = 	Math.min(foreground.getWidth(), (int)Math.ceil(renderer.getWorldViewportRightBound()) + 1);
		int topBound = 		Math.min(foreground.getHeight(), (int)Math.ceil(renderer.getWorldViewportTopBound()) + 1);
		
		for(int row = bottomBound; row < topBound; row++) {
			for(int column = leftBound; column < rightBound; column++) {
				
				if(foreground.isBlock(column, row)) {
					Model blockModel = foreground.get(column, row).getModel();
					renderer.loadOpacity(1);
					renderer.loadTransformationMatrix(column, row, 1, 1, 0);
					blockModel.render();
				}
				
				else if(background.isBlock(column, row)) {
					Model blockModel = background.get(column, row).getModel();
					renderer.loadOpacity(0.5f);
					renderer.loadTransformationMatrix(column, row, 1, 1, 0);
					blockModel.render();
				}
			}
		}
		
		for(int i = 0; i < entities.size(); i++) {
			Entity e = entities.get(i);
			
			synchronized(e) { //TODO change so entity rendering doesn't rely on physical width and height?
				if(e.getPositionX() < renderer.getWorldViewportRightBound() + e.getWidth()/2 
				&& e.getPositionX() > renderer.getWorldViewportLeftBound() - e.getWidth()/2 
				&& e.getPositionY() < renderer.getWorldViewportTopBound() + e.getHeight()/2 
				&& e.getPositionY() > renderer.getWorldViewportBottomBound() - e.getHeight()/2)
					e.render(renderer);
			}
		}
	}
}