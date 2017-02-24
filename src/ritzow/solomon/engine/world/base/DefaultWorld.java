package ritzow.solomon.engine.world.base;

import static ritzow.solomon.engine.util.Utility.combineFriction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import ritzow.solomon.engine.audio.AudioSystem;
import ritzow.solomon.engine.graphics.ModelRenderer;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.world.entity.Entity;

/**
 * Handler and organizer of {@link Entity} and {@link BlockGrid} objects. Handles updating of entities in the world and rendering of entities and blocks. 
 * Contains a foreground and background.
 */
public final class DefaultWorld extends World {
	
	/** collection of entities in the world **/
	private final List<Entity> entities;
	
	/** blocks in the world that collide with entities and and are rendered **/
	private final BlockGrid foreground, background;
	
	/** amount of downwards acceleration to apply to entities in the world **/
	private volatile float gravity;
	
	/** AudioSystem to allow entities to play sounds **/
	private volatile AudioSystem audio;
	
	public DefaultWorld(AudioSystem audio, int width, int height) {
		this(audio, width, height, 0.016f);
	}
	
	/**
	 * Initializes a new World object with a foreground, background, entity storage, and gravity.
	 * @param width the width of the foreground and background
	 * @param height the height of the foreground and background
	 * @param gravity the amount of gravity
	 */
	public DefaultWorld(AudioSystem audio, int width, int height, float gravity) {
		this.audio = audio;
		entities = new ArrayList<Entity>(100);
		foreground = new BlockGrid(width, height);
		background = new BlockGrid(width, height);
		this.gravity = gravity;
	}
	
	public DefaultWorld(byte[] data) throws ReflectiveOperationException {
		audio = null;
		gravity = ByteUtil.getFloat(data, 0);
		int foregroundLength = ByteUtil.getSerializedLength(data, 4);
		int backgroundLength = ByteUtil.getSerializedLength(data, 4 + foregroundLength);
		foreground = (BlockGrid)ByteUtil.deserialize(data, 4);
		background = (BlockGrid)ByteUtil.deserialize(data, 4 + foregroundLength);
		int numEntities = ByteUtil.getInteger(data, 4 + foregroundLength + backgroundLength);
		entities = new ArrayList<Entity>(numEntities);
		
		int index = 4 + foregroundLength + backgroundLength + 4;
		for(int i = 0; i < numEntities; i++) {
			try {
				entities.add((Entity)ByteUtil.deserialize(data, index));
			} catch(Exception ex) {
				ex.printStackTrace();
			}
			index += ByteUtil.getSerializedLength(data, index);
		}
	}
	
	@Override
	public synchronized byte[] getBytes() {
		//serialize foreground and background
		byte[] foregroundBytes = ByteUtil.serialize(foreground);
		byte[] backgroundBytes = ByteUtil.serialize(background);
		
		//number of entities currently in the world
		int numEntities = entities.size();
		
		//number of bytes of entity data
		int totalEntityBytes = 0;
		
		//array of all of the serialized entities
		byte[][] entityBytes = new byte[numEntities][];

		int index = 0;
		for(Entity e : entities) {
			try {
				byte[] bytes = ByteUtil.serialize(e);
				entityBytes[index] = bytes;
				totalEntityBytes += bytes.length;
				index++;
			} catch(Exception x) {
				numEntities--;
				System.err.println("couldn't serialize an entity: " + x.getLocalizedMessage());
			}
		}
		
		//gravity, foreground data, background data, number of entities, entity data (size)
		byte[] bytes = new byte[4 + foregroundBytes.length + backgroundBytes.length + 4 + totalEntityBytes];
		
		//write gravity
		ByteUtil.putFloat(bytes, 0, gravity);
		
		//write foreground data
		ByteUtil.copy(foregroundBytes, bytes, 4);
		
		//write background data
		ByteUtil.copy(backgroundBytes, bytes, 4 + foregroundBytes.length);
		
		//write number of entities
		ByteUtil.putInteger(bytes, 4 + foregroundBytes.length + backgroundBytes.length, numEntities);
		
		//append entity data to the end of the serialized array
		int offset = 4 + foregroundBytes.length + backgroundBytes.length + 4;
		for(byte[] entity : entityBytes) {
			if(entity != null) {
				System.arraycopy(entity, 0, bytes, offset, entity.length);
				offset += entity.length;
			}
		}
		
		return bytes;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(foreground.toString());
		builder.append(background.toString());
		
		for(Entity e : entities) {
			builder.append(e);
			builder.append('\n');
		}
		return builder.toString();
	}
	
	public BlockGrid getForeground() {
		return foreground;
	}
	
	public BlockGrid getBackground() {
		return background;
	}
	
	public void add(Entity e) {
		synchronized(entities) {
			entities.add(e);
		}
	}
	
	public void remove(Entity e) {
		synchronized(entities) {
			entities.remove(e);
		}
	}
	
	public void remove(int entityID) {
		synchronized(entities) {
			entities.removeIf(e -> e.getID() == entityID);
		}
	}
	
	public float getGravity() {
		return gravity;
	}

	public void setGravity(float gravity) {
		this.gravity = gravity;
	}

	/**
	 * Updates the entities in the world, simulating the specified amount of time. Entities below the world or marked for deletion will be removed
	 * from the world. Entities are be updated, gravity is applied, entity vs entity collisions are resolved, and entity vs block collisions
	 * are resolved.
	 * @param time the amount of time to simulate.
	 */
	public void update(float time) {
		synchronized(entities) {
			for(int i = 0; i < entities.size(); i++) {
				Entity e = entities.get(i);
				
				//remove entities that are below the world
				if(e == null || e.getPositionY() < 0 || e.getShouldDelete()) {
					entities.remove(i);
					i = Math.max(0, i - 1);
					continue;
				}
				
				//update entity position and velocity, and anything else specific to an entity
				e.update(time);
				
				//apply gravity
				e.setVelocityY(e.getVelocityY() - gravity * time);
				
				//check if collision checking is enabled
				if(e.doCollision()) {
					
					//check for entity vs. entity collisions
					for(int j = i + 1; j < entities.size(); j++) {
						Entity o = entities.get(j);
						
						if(o == null)
							continue;
						
						if(o.doCollision()) {
							boolean collision;
							
							if(e.doEntityCollisionResolution() && o.doEntityCollisionResolution()) {
								collision = (e.getMass() < o.getMass()) ? resolveCollision(e, o, time) : resolveCollision(o, e, time);
							} 
							
							else {
								collision = checkCollision(e, o);
							}
							
							if(collision) {
								e.onCollision(this, o, time);
								o.onCollision(this, e, time);
							}
						}
					}

					//Check for entity collisions with blocks
					if(e.doBlockCollisionResolution()) {
						int leftBound = Math.max(0, (int)Math.floor(e.getPositionX() - e.getWidth()));
						int topBound = Math.min(foreground.getHeight(), (int)Math.ceil(e.getPositionY() + e.getHeight()));
						int rightBound = Math.min(foreground.getWidth(), (int)Math.ceil(e.getPositionX() + e.getWidth()));
						int bottomBound = Math.max(0, (int)Math.floor(e.getPositionY() - e.getHeight()));
						
						for(int row = bottomBound; row < topBound; row++) {
							for(int column = leftBound; column < rightBound; column++) {
								if(foreground.isBlock(column, row) && foreground.get(column, row).isSolid()) {
									boolean blockUp = foreground.isBlock(column, row + 1);
									boolean blockDown = foreground.isBlock(column, row - 1);
									boolean blockLeft = foreground.isBlock(column - 1, row);
									boolean blockRight = foreground.isBlock(column + 1, row);
									if(!(blockUp && blockDown && blockLeft && blockRight)) {
										resolveBlockCollision(e, column, row, foreground.get(column, row).getFriction(), time, blockUp, blockLeft, blockRight, blockDown);
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public void render(ModelRenderer renderer) {
		renderer.loadViewMatrix(true);
		
		int leftBound = 	Math.max(0, (int)Math.floor(renderer.getWorldViewportLeftBound()));
		int bottomBound = 	Math.max(0, (int)Math.floor(renderer.getWorldViewportBottomBound()));
		int rightBound = 	Math.min(foreground.getWidth(), (int)Math.ceil(renderer.getWorldViewportRightBound()));
		int topBound = 		Math.min(foreground.getHeight(), (int)Math.ceil(renderer.getWorldViewportTopBound()));
		
		for(int row = bottomBound; row <= topBound; row++) {
			for(int column = leftBound; column <= rightBound; column++) {
				if(foreground.isBlock(column, row)) {
					renderer.renderModel(foreground.get(column, row).getModelIndex(), 1.0f, column, row, 1.0f, 1.0f, 0.0f);
				}
				
				else if(background.isBlock(column, row)) {
					renderer.renderModel(background.get(column, row).getModelIndex(), 0.5f, column, row, 1.0f, 1.0f, 0.0f);
				}
			}
		}
		
		for(int i = 0; i < entities.size(); i++) {
			Entity e = entities.get(i);
			
			if(e == null)
				continue;
			
			if(e.getPositionX() < renderer.getWorldViewportRightBound() + e.getWidth()/2 
				&& e.getPositionX() > renderer.getWorldViewportLeftBound() - e.getWidth()/2 
				&& e.getPositionY() < renderer.getWorldViewportTopBound() + e.getHeight()/2 
				&& e.getPositionY() > renderer.getWorldViewportBottomBound() - e.getHeight()/2)
						e.render(renderer);
		}
	}
	
	/**
	 * Checks if there is a collision between two entities
	 * @param e an entity
	 * @param o an entity
	 * @return true if the entities intersect eachother, false otherwise
	 */
	protected final boolean checkCollision(Entity e, Entity o) {
		return checkCollision(e, o.getPositionX(), o.getPositionY(), o.getWidth(), o.getHeight());
	}

	/**
	 * Checks if there is a collision between an entity and a hitbox
	 * @param e an entity
	 * @param otherX the x position of the hitbox
	 * @param otherY the y position of the hitbox
	 * @param otherWidth the width of the hitbox
	 * @param otherHeight the height of the hitbox
	 * @return true if the entity and hitbox intersect, false otherwise;
	 */
	protected final boolean checkCollision(Entity e, float otherX, float otherY, float otherWidth, float otherHeight) {
		 return (Math.abs(e.getPositionX() - otherX) * 2 < (e.getWidth() + otherWidth)) &&  (Math.abs(e.getPositionY() - otherY) * 2 < (e.getHeight() + otherHeight));
	}

	/**
	 * Resolves a collision between two entities. The entity passed into the first parameter will be moved if necessary.
	 * @param e the first entity, which will be moved if necessary
	 * @param o the second entity, which will not move
	 * @param time the amount of time to simulate during the collision resolution
	 * @return true if a collision occurred, false otherwise
	 */
	protected final boolean resolveCollision(Entity e, Entity o, float time) {
		return resolveCollision(e, o.getPositionX(), o.getPositionY(), o.getWidth(), o.getHeight(), o.getFriction(), time);
	}
	
	/**
	 * Resolves a collision between an entity and a hitbox. The entity passed into the first parameter will be moved if necessary.
	 * @param e the entity to be resolved
	 * @param otherX the x position of the hitbox
	 * @param otherY the y position of the hitbox
	 * @param otherWidth the width of the hitbox
	 * @param otherHeight the height of the hitbox
	 * @param otherFriction the friction of the hitbox
	 * @param time the amount of time that the resolution should simulate
	 * @return true if a collision occurred
	 */
	protected boolean resolveCollision(Entity e, float otherX, float otherY, float otherWidth, float otherHeight, float otherFriction, float time) {
		float width = 0.5f * (e.getWidth() + otherWidth);
		float height = 0.5f * (e.getHeight() + otherHeight);
		float deltaX = otherX - e.getPositionX();
		float deltaY = otherY - e.getPositionY();
		if (Math.abs(deltaX) < width && Math.abs(deltaY) < height) { /* collision! replace < in intersection detection with <= for previous behavior */
		    float wy = width * deltaY;
		    float hx = height * deltaX;
		    if (wy > hx) {
		        if (wy > -hx) { /* collision on the top of e */
		        	e.setPositionY(otherY - height);
					if(e.getVelocityY() > 0) {
						e.setVelocityY(0);
					} if(e.getVelocityX() > 0) {
		        		e.setVelocityX(Math.max(0, e.getVelocityX() - combineFriction(e.getFriction(), otherFriction) * time));
		        	} else if(e.getVelocityX() < 0) {
		        		e.setVelocityX(Math.min(0, e.getVelocityX() + combineFriction(e.getFriction(), otherFriction) * time));
		        	}
		        } else { /* collision on the left of e */
		        	e.setPositionX(otherX + width);
		        	if(e.getVelocityX() < 0) {
						e.setVelocityX(0);
					} if(e.getVelocityY() > 0) {
		        		e.setVelocityY(Math.max(0, e.getVelocityY() - combineFriction(e.getFriction(), otherFriction) * time));
		        	} else if(e.getVelocityY() < 0) {
		        		e.setVelocityY(Math.min(0, e.getVelocityY() + combineFriction(e.getFriction(), otherFriction) * time));
		        	}
		        }
		    } else {
		        if (wy > -hx) { /* collision on the right of e */
		        	e.setPositionX(otherX - width);
		        	if(e.getVelocityX() > 0) {
						e.setVelocityX(0);
					} if(e.getVelocityY() > 0) {
		        		e.setVelocityY(Math.max(0, e.getVelocityY() - combineFriction(e.getFriction(), otherFriction) * time));
		        	} else if(e.getVelocityY() < 0) {
		        		e.setVelocityY(Math.min(0, e.getVelocityY() + combineFriction(e.getFriction(), otherFriction) * time));
		        	}
		        } else { /* collision on the bottom of e */
		        	e.setPositionY(otherY + height);
		        	if(e.getVelocityY() < 0) {
		        		e.setVelocityY(0);
		        	} if(e.getVelocityX() > 0) {
		        		e.setVelocityX(Math.max(0, e.getVelocityX() - combineFriction(e.getFriction(), otherFriction) * time));
		        	} else if(e.getVelocityX() < 0) {
		        		e.setVelocityX(Math.min(0, e.getVelocityX() + combineFriction(e.getFriction(), otherFriction) * time));
		        	}
		        }
		    }
		    return true;
		}
		return false;
	}
	
	protected boolean resolveBlockCollision(Entity e, float blockX, float blockY, float blockFriction, float time, 
			boolean blockUp, boolean blockLeft, boolean blockRight, boolean blockDown) {
		float width = 0.5f * (e.getWidth() + 1);
		float height = 0.5f * (e.getHeight() + 1);
		float deltaX = blockX - e.getPositionX();
		float deltaY = blockY - e.getPositionY();
		if (Math.abs(deltaX) < width && Math.abs(deltaY) < height) { /* collision! replace < in intersection detection with <= for previous behavior */
		    float wy = width * deltaY;
		    float hx = height * deltaX;
		    if (wy > hx) {
		        if (!blockDown && wy > -hx) { /* collision on the bottom of block */
		        	e.setPositionY(blockY - height);
					if(e.getVelocityY() > 0) {
						e.setVelocityY(0);
					} if(e.getVelocityX() > 0) {
		        		e.setVelocityX(Math.max(0, e.getVelocityX() - combineFriction(e.getFriction(), blockFriction) * time));
		        	} else if(e.getVelocityX() < 0) {
		        		e.setVelocityX(Math.min(0, e.getVelocityX() + combineFriction(e.getFriction(), blockFriction) * time));
		        	}
		        } else if(!blockRight) { /* collision on right of block */
		        	e.setPositionX(blockX + width);
		        	if(e.getVelocityX() < 0) {
						e.setVelocityX(0);
					} if(e.getVelocityY() > 0) {
		        		e.setVelocityY(Math.max(0, e.getVelocityY() - combineFriction(e.getFriction(), blockFriction) * time));
		        	} else if(e.getVelocityY() < 0) {
		        		e.setVelocityY(Math.min(0, e.getVelocityY() + combineFriction(e.getFriction(), blockFriction) * time));
		        	}
		        }
		    } else {
		        if (!blockLeft && wy > -hx) { /* collision on left of block */
		        	e.setPositionX(blockX - width);
		        	if(e.getVelocityX() > 0) {
						e.setVelocityX(0);
					} if(e.getVelocityY() > 0) {
		        		e.setVelocityY(Math.max(0, e.getVelocityY() - combineFriction(e.getFriction(), blockFriction) * time));
		        	} else if(e.getVelocityY() < 0) {
		        		e.setVelocityY(Math.min(0, e.getVelocityY() + combineFriction(e.getFriction(), blockFriction) * time));
		        	}
		        } else if(!blockUp) { /* collision on top of block */
		        	e.setPositionY(blockY + height);
		        	if(e.getVelocityY() < 0) {
		        		e.setVelocityY(0);
		        	} if(e.getVelocityX() > 0) {
		        		e.setVelocityX(Math.max(0, e.getVelocityX() - combineFriction(e.getFriction(), blockFriction) * time));
		        	} else if(e.getVelocityX() < 0) {
		        		e.setVelocityX(Math.min(0, e.getVelocityX() + combineFriction(e.getFriction(), blockFriction) * time));
		        	}
		        }
		    }
		    return true;
		}
		return false;
	}

	@Override
	public Iterator<Entity> iterator() {
		return entities.iterator();
	}

	@Override
	public AudioSystem getAudioSystem() {
		return audio;
	}
	
	public void setAudioSystem(AudioSystem audio) {
		this.audio = audio;
	}
}