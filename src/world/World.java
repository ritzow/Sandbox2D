package world;

import static util.HitboxUtil.*;

import graphics.Model;
import graphics.Renderable;
import graphics.Renderer;
import java.util.ArrayList;
import world.entity.Entity;

import java.io.Serializable;

public class World implements Renderable, Serializable {	
	private static final long serialVersionUID = 8941044044393756575L;
	
	protected final ArrayList<Entity> entities;
	protected final BlockGrid blocks;
	protected final BlockGrid background;
	protected float gravity;
	
	public World(int width, int height, float gravity) {
		entities = new ArrayList<Entity>();
		blocks = new BlockGrid(this, width, height);
		background = new BlockGrid(this, width, height);
		this.gravity = gravity;
	}
	
	public BlockGrid getBlocks() {
		return blocks;
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

	public void update(float milliseconds) {
		for(int i = 0; i < entities.size(); i++) {
			Entity e = entities.get(i);
			
			//remove all entities that are below the world
			if(e == null || e.getPositionY() < 0) {
				entities.remove(i);
				i--;
				continue;
			}
			
			e.update(milliseconds);
			
			e.setVelocityY(e.getVelocityY() - gravity);
			
			//if collision is enabled on the entity
			if(!(e.getHitbox().getPriority() < 0)) {
				
				//check for entity vs. entity collisions
				for(int j = i; j < entities.size(); j++) {
					if(j > -1) {
						Entity o = entities.get(j);
						if(e != null && o != null && e != o && o.getHitbox().getPriority() >= 0) {
							if(e.getHitbox().getPriority() > o.getHitbox().getPriority()) {
								resolveCollision(e, o);
							} else {
								resolveCollision(o, e);
							}
						}
					}
				}

				//Check for entity collisions with blocks
				int leftBound = Math.max(0, (int)Math.floor(e.getPositionX() - e.getHitbox().getWidth()));
				int topBound = Math.min(blocks.getHeight(), (int)Math.ceil(e.getPositionY() + e.getHitbox().getHeight()));
				int rightBound = Math.min(blocks.getWidth(), (int)Math.ceil(e.getPositionX() + e.getHitbox().getWidth()));
				int bottomBound = Math.max(0, (int)Math.floor(e.getPositionY() - e.getHitbox().getHeight()));
				
				for(int row = bottomBound; row < topBound; row++) {
					for(int column = leftBound; column < rightBound; column++) {
						if(blocks.isBlock(column, row) && !blocks.isHidden(column, row)) {
							resolveCollision(e, column, row, 1, 1, blocks.get(column, row).getFriction());
						}
					}
				}
			}
		}
	}
	
	public boolean resolveCollision(Entity e, Entity o) {
		return resolveCollision(e, o.getPositionX(), o.getPositionY(), o.getHitbox().getWidth(), o.getHitbox().getHeight(), o.getHitbox().getFriction());
	}
	
	/** Returns true if a collision occurred **/
	public boolean resolveCollision(Entity entity, float otherX, float otherY, float otherWidth, float otherHeight, float otherFriction) {	
		float width = 0.5f * (entity.getHitbox().getWidth() + otherWidth);
		float height = 0.5f * (entity.getHitbox().getHeight() + otherHeight);
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
		        		entity.setVelocityX(Math.max(0, entity.getVelocityX() - combineFriction(entity.getHitbox().getFriction(), otherFriction)));
		        	}
		        	
		        	else if(entity.getVelocityX() < 0) {
		        		entity.setVelocityX(Math.min(0, entity.getVelocityX() + combineFriction(entity.getHitbox().getFriction(), otherFriction)));
		        	}
		        }

		        else { /* collision on the left of e */
		        	entity.setPositionX(otherX + width);
					
		        	if(entity.getVelocityX() < 0) {
						entity.setVelocityX(0);
					}
		        	
		        	if(entity.getVelocityY() > 0) {
		        		entity.setVelocityY(Math.max(0, entity.getVelocityY() - combineFriction(entity.getHitbox().getFriction(), otherFriction)));
		        	}
		        	
		        	else if(entity.getVelocityY() < 0) {
		        		entity.setVelocityY(Math.min(0, entity.getVelocityY() + combineFriction(entity.getHitbox().getFriction(), otherFriction)));
		        	}
		        }
		    }
		    
		    else {
		        if (wy > -hx) { /* collision on the right of e */
		        	entity.setPositionX(otherX - width);
					
		        	if(entity.getVelocityX() > 0) {
						entity.setVelocityX(0);;
					}
					
		        	if(entity.getVelocityY() > 0) {
		        		entity.setVelocityY(Math.max(0, entity.getVelocityY() - combineFriction(entity.getHitbox().getFriction(), otherFriction)));
		        	}
		        	
		        	else if(entity.getVelocityY() < 0) {
		        		entity.setVelocityY(Math.min(0, entity.getVelocityY() + combineFriction(entity.getHitbox().getFriction(), otherFriction)));
		        	}
		        }
		        
		        else { /* collision on the bottom of e */
		        	entity.setPositionY(otherY + height);

		        	if(entity.getVelocityY() < 0) {
		        		entity.setVelocityY(0);;
		        	}
		        	
		        	if(entity.getVelocityX() > 0) {
		        		entity.setVelocityX(Math.max(0, entity.getVelocityX() - combineFriction(entity.getHitbox().getFriction(), otherFriction)));
		        	}
		        	
		        	else if(entity.getVelocityX() < 0) {
		        		entity.setVelocityX(Math.min(0, entity.getVelocityX() + combineFriction(entity.getHitbox().getFriction(), otherFriction)));
		        	}
		        }
		    }
		    
		    return true;
		}
		
		return false;
	}
	
	@Override
	public void render(Renderer renderer) {
		renderer.loadViewMatrix(true);
		
		int leftBound = 	Math.max(0, (int)Math.floor(renderer.getWorldViewportLeftBound()));
		int rightBound = 	Math.min(blocks.getWidth(), (int)Math.ceil(renderer.getWorldViewportRightBound()) + 1);
		int topBound = 		Math.min(blocks.getHeight(), (int)Math.ceil(renderer.getWorldViewportTopBound()) + 1);
		int bottomBound = 	Math.max(0, (int)Math.floor(renderer.getWorldViewportBottomBound()));
		
		for(int row = bottomBound; row < topBound; row++) {
			for(int column = leftBound; column < Math.min(blocks.getWidth(), rightBound); column++) {
				
				if(background.isBlock(column, row) && !blocks.isBlock(column, row)) {
					Model blockModel = background.get(column, row).getModel();
					renderer.loadOpacity(0.5f);
					renderer.loadTransformationMatrix(column, row, 1, 1, 0);
					blockModel.render();
				}
				
				if(blocks.isBlock(column, row)) {
					Model blockModel = blocks.get(column, row).getModel();
					renderer.loadOpacity(1);
					renderer.loadTransformationMatrix(column, row, 1, 1, 0);
					blockModel.render();
				}
			}
		}
		
		for(int i = 0; i < entities.size(); i++) {
			Entity e = null;
			try {
				e = entities.get(i);
			} catch(IndexOutOfBoundsException exception ) {
				System.err.println("Attempted to access invalid index while rendering entities");
			}
			
			if(e != null && e.getPositionX() < renderer.getWorldViewportRightBound() + e.getHitbox().getWidth()/2 
					&& e.getPositionX() > renderer.getWorldViewportLeftBound() - e.getHitbox().getWidth()/2 
					&& e.getPositionY() < renderer.getWorldViewportTopBound() + e.getHitbox().getHeight()/2
					&& e.getPositionY() > renderer.getWorldViewportBottomBound() - e.getHitbox().getHeight()/2)
				e.render(renderer);
		}
	}
	
	public boolean entityBelow(Entity e) {
		for(int i = 0; i < entities.size(); i++) {
			Entity o = entities.get(i);
			if(o != null 
					&& e != o 
					&& o.getHitbox().getPriority() >= 0 
					&& intersection(e.getPositionX(), 
							e.getPositionY() - e.getHitbox().getHeight()/2, 
							e.getHitbox().getWidth() - 0.01f,
							0.1f, 
							o.getPositionX(), 
							o.getPositionY(), 
							o.getHitbox().getWidth(), 
							o.getHitbox().getHeight())) {
				return true;
			}
		}
		return false;
	}
	
	public boolean blockBelow(Entity e) {
		return blocks.isBlock(e.getPositionX() - e.getHitbox().getWidth()/2 + 0.01f, e.getPositionY() - e.getHitbox().getHeight()/2 - 0.05f) || 
			   blocks.isBlock(e.getPositionX() + e.getHitbox().getWidth()/2 - 0.01f, e.getPositionY() - e.getHitbox().getHeight()/2 - 0.05f);
	}
	
	public boolean blockLeft(Entity e) {
		return blocks.isBlock(e.getPositionX() - e.getHitbox().getWidth()/2 - 0.01f, e.getPositionY() + e.getHitbox().getHeight()/2 - 0.05f) || 
			   blocks.isBlock(e.getPositionX() - e.getHitbox().getWidth()/2 - 0.01f, e.getPositionY() - e.getHitbox().getHeight()/2 + 0.05f);
	}
	
	public boolean blockRight(Entity e) {
		return blocks.isBlock(e.getPositionX() + e.getHitbox().getWidth()/2 + 0.01f, e.getPositionY() + e.getHitbox().getHeight()/2 - 0.05f) || 
			   blocks.isBlock(e.getPositionX() + e.getHitbox().getWidth()/2 + 0.01f, e.getPositionY() - e.getHitbox().getHeight()/2 + 0.05f);
	}
}