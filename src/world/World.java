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
	protected float gravity;
	
	protected float leftBarrier;
	protected float rightBarrier;
	protected float topBarrier;
	protected float bottomBarrier;
	
	public World(int width, int height, float gravity) {
		entities = new ArrayList<Entity>();
		blocks = new BlockGrid(this, width, height);
		leftBarrier = 0;
		rightBarrier = width - 1;
		topBarrier = height;
		bottomBarrier = 0;
		this.gravity = gravity;
	}
	
	public void setBarrier(float left, float right, float top, float bottom) {
		this.leftBarrier = left;
		this.rightBarrier = right;
		this.topBarrier = top;
		this.bottomBarrier = bottom;
	}
	
	public void resetBarrier() {
		this.leftBarrier = 0;
		this.rightBarrier = blocks.getWidth();
		this.topBarrier = blocks.getHeight();
		this.bottomBarrier = 0;
	}
	
	public BlockGrid getBlocks() {
		return blocks;
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
			if(e == null) continue;
			
			//remove all entities that are below the world
			if(e.position().getY() < 0) {
				entities.set(i, null);
				entities.remove(i);
				i--;    
			}
			
			e.update(milliseconds);
			
			e.velocity().setAccelerationY(-gravity);
			
			if(e.position().getX() <= leftBarrier)
				e.velocity().setAccelerationX(e.velocity().getAccelerationX() + gravity * 30);
			else if(e.position().getX() >= rightBarrier)
				e.velocity().setAccelerationX(e.velocity().getAccelerationX() - gravity * 30);
			if(e.position().getY() <= bottomBarrier)
				e.velocity().setAccelerationY(e.velocity().getAccelerationY() + gravity * 30);
			else if(e.position().getY() >= topBarrier)
				e.velocity().setAccelerationY(e.velocity().getY() - gravity * 30);
			
			if(e.getHitbox().getPriority() >= 0) {
				for(int j = i; j < entities.size(); j++) { //check for entity vs. entity collisions
					Entity o = entities.get(j);
					if(e != null && o != null && e != o && o.getHitbox().getPriority() >= 0) {
						if(e.getHitbox().getPriority() > o.getHitbox().getPriority()) {
							resolveCollision(e, o);
						} else {
							resolveCollision(o, e);
						}
					}
				}

				//Check for entity collisions with blocks, starting in the bottom left, moving to the top right
				int leftBound = Math.max(0, (int)Math.floor(e.position().getX() - e.getHitbox().getWidth()));
				int topBound = Math.min(blocks.getHeight(), (int)Math.ceil(e.position().getY() + e.getHitbox().getHeight()));
				int rightBound = Math.min(blocks.getWidth(), (int)Math.ceil(e.position().getX() + e.getHitbox().getWidth()));
				int bottomBound = Math.max(0, (int)Math.floor(e.position().getY() - e.getHitbox().getHeight()));
				
				for(int row = bottomBound; row < topBound; row++) {
					for(int column = leftBound; column < rightBound; column++) {
						if(blocks.isBlock(column, row) && !blocks.isHidden(column, row)) {
							resolveCollision(e, column, row, 1, 1, (blocks.get(column, row).getFriction() + e.getHitbox().getFriction())/2);
						}
					}
				}
			}
		}
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
			
			if(e != null && e.position().getX() < renderer.getWorldViewportRightBound() + e.getHitbox().getWidth()/2 
					&& e.position().getX() > renderer.getWorldViewportLeftBound() - e.getHitbox().getWidth()/2 
					&& e.position().getY() < renderer.getWorldViewportTopBound() + e.getHitbox().getHeight()/2
					&& e.position().getY() > renderer.getWorldViewportBottomBound() - e.getHitbox().getHeight()/2)
				e.render(renderer);
		}
	}
	
	public boolean resolveCollision(Entity e, Entity o) {
		return resolveCollision(e, 
				o.position().getX(),
				o.position().getY(), 
				o.getHitbox().getWidth(), 
				o.getHitbox().getHeight(), 
				combineFriction(e.getHitbox().getFriction(), o.getHitbox().getFriction()));
	}
	
	/** Returns true if a collision occurred **/
	public boolean resolveCollision(Entity entity, float otherX, float otherY, float otherWidth, float otherHeight, float friction) {	
		float width = 0.5f * (entity.getHitbox().getWidth() + otherWidth);
		float height = 0.5f * (entity.getHitbox().getHeight() + otherHeight);
		float deltaX = otherX - entity.position().getX();
		float deltaY = otherY - entity.position().getY();
		if (Math.abs(deltaX) < width && Math.abs(deltaY) < height) { /* collision! replace < in intersection detection with <= for previous behavior */
		    float wy = width * deltaY;
		    float hx = height * deltaX;
		    if (wy > hx) {
		        if (wy > -hx) { /* collision on the top of e */
		        	entity.position().setY(otherY - height);
		        	
					if(entity.velocity().getY() > 0) {
						entity.velocity().setY(0);
					}
					
					if(entity.velocity().getAccelerationY() > 0) {
						entity.velocity().setAccelerationY(0);
					}
					
		        	if(entity.velocity().getAccelerationX() > 0) {
		        		entity.velocity().setAccelerationX(Math.max(0, entity.velocity().getAccelerationX() - friction));
		        	}
		        	
		        	else if(entity.velocity().getAccelerationX() < 0) {
		        		entity.velocity().setAccelerationX(Math.min(0, entity.velocity().getAccelerationX() + friction));
		        	}
		        }

		        else { /* collision on the left of e */
		        	entity.position().setX(otherX + width);
					
		        	if(entity.velocity().getX() < 0) {
						entity.velocity().setX(0);
					}
					
		        	if(entity.velocity().getAccelerationX() < 0) {
		        		entity.velocity().setAccelerationX(0);
		        	}
		        	
		        	if(entity.velocity().getAccelerationY() > 0) {
		        		entity.velocity().setAccelerationY(Math.max(0, entity.velocity().getAccelerationY() - friction));
		        	}
		        	
		        	else if(entity.velocity().getAccelerationY() < 0) {
		        		entity.velocity().setAccelerationY(Math.min(0, entity.velocity().getAccelerationY() + friction));
		        	}
		        }
		    }
		    
		    else {
		        if (wy > -hx) { /* collision on the right of e */
		        	entity.position().setX(otherX - width);
					
		        	if(entity.velocity().getX() > 0) {
						entity.velocity().setX(0);
					}
					
					if(entity.velocity().getAccelerationX() > 0) {
						entity.velocity().setAccelerationX(0);
					}
					
					if(entity.velocity().getAccelerationY() > 0) {
						entity.velocity().setAccelerationY(Math.max(0, entity.velocity().getAccelerationY() - friction));
					}
					
					else if(entity.velocity().getAccelerationY() < 0) {
						entity.velocity().setAccelerationY(Math.min(0, entity.velocity().getAccelerationY() + friction));
					}
		        }
		        
		        else { /* collision on the bottom of e */
		        	entity.position().setY(otherY + height);
		        	
		        	if(entity.velocity().getY() < 0) {
		        		entity.velocity().setY(0);
		        	}
		        	
		        	if(entity.velocity().getAccelerationY() < 0) {
		        		entity.velocity().setAccelerationY(0);
		        	}
		        	
		        	if(entity.velocity().getAccelerationX() > 0) {
		        		entity.velocity().setAccelerationX(Math.max(0, entity.velocity().getAccelerationX() - friction));
		        	}
		        	
		        	else if(entity.velocity().getAccelerationX() < 0) {
		        		entity.velocity().setAccelerationX(Math.min(0, entity.velocity().getAccelerationX() + friction));
		        	}
		        }
		    }
		    
		    return true;
		}
		
		return false;
	}
	
	public boolean entityBelow(Entity e) {
		for(int i = 0; i < entities.size(); i++) {
			Entity o = entities.get(i);
			if(o != null 
					&& e != o 
					&& o.getHitbox().getPriority() >= 0 
					&& intersection(e.position().getX(), 
							e.position().getY() - e.getHitbox().getHeight()/2, 
							e.getHitbox().getWidth() - 0.01f,
							0.1f, 
							o.position().getX(), 
							o.position().getY(), 
							o.getHitbox().getWidth(), 
							o.getHitbox().getHeight())) {
				return true;
			}
		}
		return false;
	}
	
	public boolean blockBelow(Entity e) {
		return blocks.isBlock(e.position().getX() - e.getHitbox().getWidth()/2 + 0.01f, e.position().getY() - e.getHitbox().getHeight()/2 - 0.05f) || 
			   blocks.isBlock(e.position().getX() + e.getHitbox().getWidth()/2 - 0.01f, e.position().getY() - e.getHitbox().getHeight()/2 - 0.05f);
	}
	
	public boolean blockLeft(Entity e) {
		return blocks.isBlock(e.position().getX() - e.getHitbox().getWidth()/2 - 0.01f, e.position().getY() + e.getHitbox().getHeight()/2 - 0.05f) || 
			   blocks.isBlock(e.position().getX() - e.getHitbox().getWidth()/2 - 0.01f, e.position().getY() - e.getHitbox().getHeight()/2 + 0.05f);
	}
	
	public boolean blockRight(Entity e) {
		return blocks.isBlock(e.position().getX() + e.getHitbox().getWidth()/2 + 0.01f, e.position().getY() + e.getHitbox().getHeight()/2 - 0.05f) || 
			   blocks.isBlock(e.position().getX() + e.getHitbox().getWidth()/2 + 0.01f, e.position().getY() - e.getHitbox().getHeight()/2 + 0.05f);
	}
}