package ritzow.sandbox.world;

import static ritzow.sandbox.util.Utility.combineFriction;
import static ritzow.sandbox.util.Utility.intersection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Predicate;
import ritzow.sandbox.audio.AudioSystem;
import ritzow.sandbox.util.ByteUtil;
import ritzow.sandbox.util.DataReader;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.entity.Entity;

public abstract class AbstractWorld implements World {
	
	/** collection of entities in the world **/
	private final List<Entity> entities;
	
	/** queues for entities to be added or removed from the world **/
	private final Queue<Entity> entityAddQueue, entityRemoveQueue;
	
	/** blocks in the world that collide with entities and and are rendered **/
	protected final BlockGrid foreground, background;
	
	/** AudioSystem to allow entities to play sounds **/
	protected volatile AudioSystem audio;
	
	/** amount of downwards acceleration to apply to entities in the world **/
	private volatile float gravity;
	
	/** Entity ID counter **/
	private volatile int lastEntityID;
	
	/**
	 * Initializes a new World object with a foreground, background, entity storage, and gravity.
	 * @param width the width of the foreground and background
	 * @param height the height of the foreground and background
	 * @param gravity the amount of gravity
	 */
	public AbstractWorld(AudioSystem audio, int width, int height, float gravity) {
		this.audio = audio;
		entities = new ArrayList<Entity>(100);
		this.entityAddQueue = new LinkedList<Entity>();
		this.entityRemoveQueue = new LinkedList<Entity>();
		foreground = new BlockGrid(width, height);
		background = new BlockGrid(width, height);
		this.gravity = gravity;
	}
	
	public AbstractWorld(DataReader reader) {
		audio = null;
		entityAddQueue = new LinkedList<Entity>();
		entityRemoveQueue = new LinkedList<Entity>();
		gravity = reader.readFloat();
		foreground = reader.readObject();
		background = reader.readObject();
		int entityCount = reader.readInteger();
		entities = new ArrayList<Entity>(entityCount);
		for(int i = 0; i < entityCount; i++) {
			entities.add(reader.readObject());
		}
		lastEntityID = reader.readInteger();
	}
	
	public AbstractWorld(byte[] data) throws ReflectiveOperationException {
		audio = null;
		entityAddQueue = new LinkedList<Entity>();
		entityRemoveQueue = new LinkedList<Entity>();
		
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
		
		lastEntityID = ByteUtil.getInteger(data, index);
	}
	
	public final byte[] getBytes(Predicate<Entity> entityFilter) {
		//serialize foreground and background
		byte[] foregroundBytes = ByteUtil.serialize(foreground);
		byte[] backgroundBytes = ByteUtil.serialize(background);
		
		synchronized(entities) {
			int entityIDCounter = this.lastEntityID; //TODO this entire method is kinda unsafe
			
			//number of entities currently in the world
			int numEntities = entities.size();
			
			//number of bytes of entity data
			int totalEntityBytes = 0;
			
			//array of all of the serialized entities
			byte[][] entityBytes = new byte[numEntities][];

			int index = 0;
			for(Entity e : entities) {
				if(entityFilter.test(e) == true) {
					try {
						byte[] bytes = ByteUtil.serialize(e);
						entityBytes[index] = bytes;
						totalEntityBytes += bytes.length;
						index++;
					} catch(Exception x) {
						numEntities--;
						System.err.println("couldn't serialize an entity: " + x.getLocalizedMessage());
					}
				} else {
					numEntities--;
				}
			}
			
			//gravity, foreground data, background data, number of entities, entity data (size), lastEntityID
			byte[] bytes = new byte[4 + foregroundBytes.length + backgroundBytes.length + 4 + totalEntityBytes + 4];
			
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
			
			ByteUtil.putInteger(bytes, offset, entityIDCounter);
			
			return bytes;
		}
	}
	
	@Override
	public final byte[] getBytes() { //needed for saving world to file as Transportable
		return getBytes(e -> true);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder().append(foreground.toString()).append(background.toString());
		for(Entity e : entities) {
			builder.append(e).append('\n');
		}
		return builder.toString();
	}
	
	public int nextEntityID() {
		return ++lastEntityID;
	}
	
	@Override
	public final BlockGrid getForeground() {
		return foreground;
	}
	
	@Override
	public final BlockGrid getBackground() {
		return background;
	}
	
	public void forEach(Consumer<Entity> consumer) {
		synchronized(entities) {
			entities.forEach(consumer);
		}
	}
	
	/**
	 * Returns a collection of entities that are partially or fully within the given rectangle bounds
	 * @param x the center x coordinate
	 * @param y the center y coordinate
	 * @param width the width of the rectangle
	 * @param height the height of the rectangle
	 * @return
	 */
	public Collection<Entity> getEntitiesInRectangle(float x, float y, float width, float height) {
		Collection<Entity> col = null;
		synchronized(entities) {
			for(Entity e : entities) {
				if(intersection(x, y, width, height, e.getPositionX(), e.getPositionY(), e.getWidth(), e.getHeight())) {
					if(col == null) {
						col = new ArrayList<Entity>();
						col.add(e);
					}
				}
			}
		}
		return col == null ? Collections.emptyList() : col;
	}

	@Override
	public final AudioSystem getAudioSystem() {
		return audio;
	}
	
	public final void setAudioSystem(AudioSystem audio) {
		this.audio = audio;
	}
	
	/**
	 * Non-thread-safe version of queueAdd that will add the entity to the world immediately
	 * @param e the entity to add
	 */
	public final void add(Entity e) {
		synchronized(entities) {
			entities.add(e);
			onEntityAdd(e);
		}
	}
	
	/**
	 * Non-thread-safe version of queueRemove that will remove the entity from the world immediately
	 * @param e the entity to add
	 */
	public final void remove(Entity e) {
		synchronized(entities) {
			entities.remove(e);
			onEntityRemove(e);
		}
	}
	
	public final void queueAdd(Entity e) {
		synchronized(entityAddQueue) {
			entityAddQueue.add(e);
		}
	}
	
	public final void queueRemove(Entity e) {
		synchronized(entityRemoveQueue) {
			entityRemoveQueue.add(e);
		}
	}
	
	public final float getGravity() {
		return gravity;
	}

	public final void setGravity(float gravity) {
		this.gravity = gravity;
	}
	
	public final Entity find(int entityID) { //TODO I dobut this is thread-safe or efficient
		synchronized(entities) {
			for(Entity e : entities) {
				if(e.getID() == entityID) {
					return e;
				}
			}
		}
		return null;
	}
	
	protected void onEntityAdd(Entity e) {}
	protected void onEntityRemove(Entity e) {}
	protected void onEntityUpdate(Entity e) {}

	/**
	 * Updates the entities in the world, simulating the specified amount of time. Entities below the world or marked for deletion will be removed
	 * from the world. Entities are be updated, gravity is applied, entity vs entity collisions are resolved, and entity vs block collisions
	 * are resolved.
	 * @param time the amount of time to simulate.
	 */
	@Override
	public final void update(float time) {
		synchronized(entities) {
			synchronized(entityAddQueue) {
				entities.addAll(entityAddQueue);
				for(Entity e : entityAddQueue) {
					onEntityAdd(e);
				}
				entityAddQueue.clear();	
			} synchronized(entityRemoveQueue) {
				entities.removeAll(entityRemoveQueue);
				for(Entity e : entityRemoveQueue) {
					onEntityRemove(e);
				}
				entityRemoveQueue.clear();
			}
			
			for(int i = 0; i < entities.size(); i++) {
				Entity e = entities.get(i);
				
				//remove entities that are below the world or are flagged for deletion
				if(e == null || e.getPositionY() < 0 || e.getShouldDelete()) {
					onEntityRemove(entities.remove(i));
					i = Math.max(0, i - 1);
					continue;
				}
				
				//update entity position and velocity, and anything else specific to an entity
				e.update(time);
				
				//apply gravity
				e.setVelocityY(e.getVelocityY() - gravity * time);
				
				//check if collision checking is enabled
				if(e.doCollision()) {
					
					//check for entity vs. entity collisions with all entities that have not already been collision checked with (for first element, all entites, for last, no entities)
					for(int j = i + 1; j < entities.size(); j++) {
						Entity o = entities.get(j);
						if(o != null && o.doCollision()) {
							boolean collision;
							
							if(e.doEntityCollisionResolution() && o.doEntityCollisionResolution()) {
								//TODO improve collision priority/interaction (should both entities move in opposite directions?)
								collision = (e.getMass() <= o.getMass() || e.getID() < o.getID()) ? resolveCollision(e, o, time) : resolveCollision(o, e, time);
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
					if(e.doBlockCollisionResolution()) {
						int leftBound = Math.max(0, (int)Math.floor(e.getPositionX() - e.getWidth()));
						int topBound = Math.min(foreground.getHeight(), (int)Math.ceil(e.getPositionY() + e.getHeight()));
						int rightBound = Math.min(foreground.getWidth(), (int)Math.ceil(e.getPositionX() + e.getWidth()));
						int bottomBound = Math.max(0, (int)Math.floor(e.getPositionY() - e.getHeight()));
						
						for(int row = bottomBound; row < topBound; row++) {
							for(int column = leftBound; column < rightBound; column++) {
								Block block = foreground.get(column, row);
								if(foreground.isBlock(column, row) && block.isSolid()) {
									boolean blockUp = foreground.isBlock(column, row + 1);
									boolean blockDown = foreground.isBlock(column, row - 1);
									boolean blockLeft = foreground.isBlock(column - 1, row);
									boolean blockRight = foreground.isBlock(column + 1, row);
									if(!(blockUp && blockDown && blockLeft && blockRight)) {
										resolveBlockCollision(this, e, block, column, row, time, blockUp, blockLeft, blockRight, blockDown);
									}
								}
							}
						}
					}
				}
				onEntityUpdate(e);
			}
		}
	}
	
	/**
	 * Checks if there is a collision between two entities
	 * @param e an entity
	 * @param o an entity
	 * @return true if the entities intersect eachother, false otherwise
	 */
	protected final static boolean checkCollision(Entity e, Entity o) {
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
	protected final static boolean checkCollision(Entity e, float otherX, float otherY, float otherWidth, float otherHeight) {
		 return (Math.abs(e.getPositionX() - otherX) * 2 < (e.getWidth() + otherWidth)) &&  (Math.abs(e.getPositionY() - otherY) * 2 < (e.getHeight() + otherHeight));
	}

	/**
	 * Resolves a collision between two entities. The entity passed into the first parameter will be moved if necessary.
	 * @param e the first entity, which will be moved if necessary
	 * @param o the second entity, which will not move
	 * @param time the amount of time to simulate during the collision resolution
	 * @return true if a collision occurred, false otherwise
	 */
	protected final static boolean resolveCollision(Entity e, Entity o, float time) {
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
	protected static boolean resolveCollision(Entity e, float otherX, float otherY, float otherWidth, float otherHeight, float otherFriction, float time) {
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
					}
		        }
		    } else {
		        if (wy > -hx) { /* collision on the right of e */
		        	e.setPositionX(otherX - width);
		        	if(e.getVelocityX() > 0) {
						e.setVelocityX(0);
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
	
	protected static boolean resolveBlockCollision(World world, Entity e, Block block, float blockX, float blockY, float time, 
			boolean blockUp, boolean blockLeft, boolean blockRight, boolean blockDown) {
		float width = 0.5f * (e.getWidth() + 1);
		float height = 0.5f * (e.getHeight() + 1);
		float deltaX = blockX - e.getPositionX();
		float deltaY = blockY - e.getPositionY();
		if (Math.abs(deltaX) < width && Math.abs(deltaY) < height) { /* collision! */
		    float wy = width * deltaY;
		    float hx = height * deltaX;
		    if (wy > hx) {
		        if (!blockDown && wy > -hx) { /* collision on the bottom of block */
		        	e.setPositionY(blockY - height);
					if(e.getVelocityY() > 0) {
						e.setVelocityY(0);
					} if(e.getVelocityX() > 0) {
		        		e.setVelocityX(Math.max(0, e.getVelocityX() - combineFriction(e.getFriction(), block.getFriction()) * time));
		        	} else if(e.getVelocityX() < 0) {
		        		e.setVelocityX(Math.min(0, e.getVelocityX() + combineFriction(e.getFriction(), block.getFriction()) * time));
		        	}
		        } else if(!blockRight) { /* collision on right of block */
		        	e.setPositionX(blockX + width);
		        	if(e.getVelocityX() < 0) {
						e.setVelocityX(0);
					}
		        }
		    } else {
		        if (!blockLeft && wy > -hx) { /* collision on left of block */
		        	e.setPositionX(blockX - width);
		        	if(e.getVelocityX() > 0) {
						e.setVelocityX(0);
					}
		        } else if(!blockUp) { /* collision on top of block */
		        	e.setPositionY(blockY + height);
		        	if(e.getVelocityY() < 0) {
		        		e.setVelocityY(0);
		        	} if(e.getVelocityX() > 0) {
		        		e.setVelocityX(Math.max(0, e.getVelocityX() - combineFriction(e.getFriction(), block.getFriction()) * time));
		        	} else if(e.getVelocityX() < 0) {
		        		e.setVelocityX(Math.min(0, e.getVelocityX() + combineFriction(e.getFriction(), block.getFriction()) * time));
		        	}
		        }
		    }
			e.onCollision(world, block, blockX, blockY, time);
		    return true;
		}
		return false;
	}
}