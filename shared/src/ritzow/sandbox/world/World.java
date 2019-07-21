package ritzow.sandbox.world;

import static ritzow.sandbox.util.Utility.intersection;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.data.Transportable;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.entity.Entity;

/**
 * A World instance manages a foreground and background BlockGrid, and a collection of entities.
 * @author Solomon Ritzow
 *
 */
public class World implements Transportable, Iterable<Entity> {
	
	private static final float GRAVITY = Utility.convertAccelerationSecondsNanos(9.8f * 3);

	//Entity operations:
	//Get an entity by ID
	//Remove an entity by ID or by object
	//Add an entity
	//iterate over entities and remaining entities
	//Remove entities while iterating
	
	/** collection of entities in the world **/
	private final List<Entity> entities;
	private final Map<Integer, Entity> entitiesID;

	/** blocks in the world that collide with entities and and are rendered **/
	private final BlockGrid foreground, background;

	/** Entity ID counter **/
	private int lastEntityID;

	/** called when an entity is removed from the world **/
	private Consumer<Entity> onRemove;

	/** For access protection during entity updates **/
	private boolean isEntitiesModifiable = true;

	/**
	 * Initializes a new World object with a foreground, background, entity storage, and gravity.
	 * @param width the width of the foreground and background
	 * @param height the height of the foreground and background
	 * @param gravity the amount of gravity
	 */
	public World(int width, int height, float gravity) {
		entities = new ArrayList<>();
		entitiesID = new HashMap<>();
		foreground = new BlockGrid(width, height);
		background = new BlockGrid(width, height);
	}

	public World(TransportableDataReader reader) {
		foreground = Objects.requireNonNull(reader.readObject());
		background = Objects.requireNonNull(reader.readObject());
		int entityCount = reader.readInteger();
		entities = new ArrayList<>(entityCount);
		entitiesID = new HashMap<>();
		for(int i = 0; i < entityCount; i++) {
			addEntity(Objects.requireNonNull(reader.readObject()));
		}
		lastEntityID = reader.readInteger();
	}

	@Override
	public final byte[] getBytes(Serializer ser) { //needed for saving world to file as Transportable
		return getBytesFiltered(e -> true, ser);
	}

	public final byte[] getBytesFiltered(Predicate<Entity> entityFilter, Serializer ser) {
		//serialize foreground and background
		byte[] foregroundBytes = ser.serialize(foreground);
		byte[] backgroundBytes = ser.serialize(background);

		int entityIDCounter = this.lastEntityID;

		//number of entities currently in the world
		int numEntities = entities.size();

		//number of bytes of entity data
		int totalEntityBytes = 0;

		//array of all of the serialized entities
		byte[][] entityBytes = new byte[numEntities][];

		for(int i = 0; i < numEntities; ++i) {
			Entity e = entities.get(i);
			if(entityFilter.test(e)) {
				try {
					byte[] bytes = ser.serialize(e);
					entityBytes[i] = bytes;
					totalEntityBytes += bytes.length;
				} catch(Exception x) {
					numEntities--;
					i--;
					System.err.println("couldn't serialize an entity: " + x.getLocalizedMessage());
				}
			} else {
				numEntities--;
			}
		}

		//gravity, foreground data, background data, number of entities, entity data (size), lastEntityID
		byte[] bytes = new byte[4 + foregroundBytes.length + backgroundBytes.length + 4 + totalEntityBytes + 4];
		int index = 0;

		//write foreground data
		Bytes.copy(foregroundBytes, bytes, index);
		index += foregroundBytes.length;

		//write background data
		Bytes.copy(backgroundBytes, bytes, index);
		index += backgroundBytes.length;

		//write number of entities
		Bytes.putInteger(bytes, index, numEntities);
		index += 4;

		//append entity data to the end of the serialized array
		int entityByteData = Bytes.concatenate(bytes, index, entityBytes);
		index += entityByteData;

		//write last entity id
		Bytes.putInteger(bytes, index, entityIDCounter);

		return bytes;

	}

	@Override
	public String toString() {
		return new StringJoiner("", "Entity Count: ", "")
			.add(Integer.toString(entities.size()))
			.add("\n")
			.add(foreground.toString())
			.add("\n")
			.add(background.toString())
			.toString();
	}

	/**
	 * Provides a unique entity identifier than can be used to identify a single entity.
	 * @return an entity identifier.
	 */
	public int nextEntityID() {
		return ++lastEntityID;
	}

	/**
	 * Get the foreground {@code BlockGrid} of the world, which interacts physically with entities.
	 */
	public final BlockGrid getForeground() {
		return foreground;
	}

	/**
	 * Get the background {@code BlockGrid} of the world.
	 */
	public final BlockGrid getBackground() {
		return background;
	}

	/**
	 * Enables entity removal and provides an action to take when the world removes entities when updated
	 * @param onRemove action to take when an entity is removed
	 */
	public void setRemoveEntities(Consumer<Entity> onRemove) {
		this.onRemove = onRemove;
	}

	/**
	 * Enables entity removal.
	 * @see setRemoveEntities(Consumer)
	 */
	public void setRemoveEntities() {
		this.onRemove = e -> {};
	}

	public void removeIf(Predicate<Entity> predicate) {
		Iterator<Entity> it = entities.iterator();
		while(it.hasNext()) {
			Entity next = it.next();
			if(predicate.test(next)) {
				it.remove();
				if(onRemove != null) onRemove.accept(next);
			}
		}
	}

	public int entities() {
		return entities.size();
	}

	public boolean contains(Entity e) {
		return entities.contains(e);
	}

	@Override
	public Iterator<Entity> iterator() {
		return entities.iterator();
	}

	@Override
	public Spliterator<Entity> spliterator() {
		return entities.spliterator();
	}

	@Override
	public void forEach(Consumer<? super Entity> consumer) {
		entities.forEach(consumer);
	}
	
	public Entity getEntityFromID(int id) {
		return entitiesID.get(id);
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
		for(Entity e : entities) {
			if(intersection(x, y, width, height, e.getPositionX(), e.getPositionY(), e.getWidth(), e.getHeight())) {
				if(col == null) {
					col = new ArrayList<>();
					col.add(e);
				}
			}
		}
		return col == null ? Collections.emptyList() : col;
	}

	private void checkEntitiesModifiable() {
		if(!isEntitiesModifiable)
			throw new IllegalStateException("cannot add/remove from world: world is being updated");
	}
	
	private void addEntity(Entity e) {
		entities.add(e);
		entitiesID.put(e.getID(), e);
	}
	
	private void removeEntity(Entity e) {
		entities.remove(e);
		entitiesID.remove(e.getID());
	}

	/**
	 * Adds the provided non-null Entity to the world.
	 * @param e the entity to add.
	 */
	public final void add(Entity e) {
		checkEntitiesModifiable();
		addEntity(Objects.requireNonNull(e));
	}

	/**
	 * Removes the provided entity from the world.
	 * @param e the entity to remove.
	 */
	public final void remove(Entity e) {
		checkEntitiesModifiable();
		removeEntity(e);
	}

	/**
	 * Updates the entities in the world, simulating a single timestep of the provided amount.
	 * Entities are updated, gravity is applied, entity vs entity collisions are resolved,
	 * and entity vs block collisions are resolved. If {@code setRemoveEntities has been called},
	 * entities that are below the bottom of the world will be removed and, if provided, the entity
	 * remove handler will be called.
	 * @param time the amount of time to simulate.
	 */
	public final void update(long nanoseconds) {
		isEntitiesModifiable = false;
		var entities = this.entities;
		int size = entities.size();
		for(int i = 0; i < size; i++) {
			Entity e = entities.get(i);
			//remove entities that are below the world or are flagged for deletion
			if(onRemove != null && e.getPositionY() < 0 || e.getShouldDelete()) {
				onRemove.accept(entities.remove(i));
				size--;
			} else {
				//update entity position and velocity, and anything else specific to an entity
				e.update(this, nanoseconds);

				//apply gravity
				e.setVelocityY(e.getVelocityY() - GRAVITY * nanoseconds);

				//TODO fix getting stuck on block edges when movement during jumping is disabled.
				
				//check for entity vs. entity collisions with all entities that have not already been
				//collision checked with (for first element, all entites, for last, no entities)
				for(int j = i + 1; j < size; j++) {
					resolveEntityCollision(e, entities.get(j), nanoseconds);
				}

				//Check for entity collisions with blocks
				if(e.collidesWithBlocks()) {
					resolveBlockCollisions(e, nanoseconds);
				}
			}
		}
		isEntitiesModifiable = true;
	}

	private void resolveBlockCollisions(Entity e, long nanoseconds) {
		BlockGrid foreground = this.foreground;
		int worldTop = foreground.getHeight();
		int worldRight = foreground.getWidth();
		int leftBound = Utility.clampLowerBound(0, e.getPositionX() - e.getWidth());
		int topBound = Utility.clampUpperBound(worldTop, e.getPositionY() + e.getHeight());
		int rightBound = Utility.clampUpperBound(worldRight, e.getPositionX() + e.getWidth());
		int bottomBound = Utility.clampLowerBound(0, e.getPositionY() - e.getHeight());
		//TODO is there redundancy between this and resolveBlockCollision
		//System.out.println(leftBound + ", " + bottomBound + ", " + rightBound + ", " + topBound);
		for(int row = bottomBound; row < topBound; row++) {
			for(int column = leftBound; column < rightBound; column++) {
				Block block = foreground.get(column, row);
				if(block != null && block.isSolid()) {
					resolveBlockCollision(e, block, column, row, nanoseconds);
				}
			}
		}
	}

	/**
	 * Resolves a collision between an entity and a hitbox. 
	 * The entity passed into the first parameter will be moved if necessary.
	 * @param e the entity to be resolved
	 * @param otherX the x position of the hitbox
	 * @param otherY the y position of the hitbox
	 * @param otherWidth the width of the hitbox
	 * @param otherHeight the height of the hitbox
	 * @param otherFriction the friction of the hitbox
	 * @param time the amount of time that the resolution should simulate
	 * @return true if a collision occurred
	 */
	private void resolveEntityCollision(Entity e, Entity o, long nanoseconds) {
		if(e.hasCollision() || o.hasCollision()) {
			//TODO use momentum (mass * velocity) to determine which one moves
			float width = 0.5f * (e.getWidth() + o.getWidth());
			float height = 0.5f * (e.getHeight() + o.getHeight());
			float deltaX = o.getPositionX() - e.getPositionX();
			float deltaY = o.getPositionY() - e.getPositionY();
			
			if(Math.abs(deltaX) < width && Math.abs(deltaY) < height) {
				//collision! replace < in intersection detection with <= for previous behavior
				
				e.onCollision(this, o, nanoseconds);
				o.onCollision(this, e, nanoseconds);
				
				if(e.collidesWithEntities() && o.collidesWithEntities()) {
					float wy = width * deltaY;
				    float hx = height * deltaX;
				    if (wy > hx) {
				        if (wy > -hx) { /* collision on the bottom of other */
				        	e.setPositionY(o.getPositionY() - height);
				        	onCollisionEntity(e, o.getFriction(), nanoseconds);
				        } else { /* collision on the right of other */
				        	e.setPositionX(o.getPositionX() + width);
				        	if(e.getVelocityX() < 0) {
								e.setVelocityX(0);
							}
				        }
				    } else {
				        if (wy > -hx) { /* collision on the left of other */
				        	e.setPositionX(o.getPositionX() - width);
				        	if(e.getVelocityX() > 0) {
								e.setVelocityX(0);
							}
				        } else { /* collision on the top of other */
				        	e.setPositionY(o.getPositionY() + height);
				        	onCollisionEntity(e, o.getFriction(), nanoseconds);
				        }
				    }
				}
			}	
		}
	}

	private void resolveBlockCollision(Entity e, Block block, int blockX, int blockY, long nanoseconds) {
		float centroidX = 0.5f * (e.getWidth() + 1); //average between entity and block
		float centroidY = 0.5f * (e.getHeight() + 1);
		float deltaX = blockX - e.getPositionX();
		float deltaY = blockY - e.getPositionY();
		if (Math.abs(deltaX) < centroidX && Math.abs(deltaY) < centroidY) { /* collision! */
			e.onCollision(this, block, blockX, blockY, nanoseconds);
		    float wy = centroidX * deltaY;
		    float hx = centroidY * deltaX;
		    if (wy > hx) {
		        if (wy > -hx) { /* collision on the bottom of block, top of entity */
		        	e.setPositionY(blockY - centroidY);
		        	onCollisionEntity(e, block.getFriction(), nanoseconds);
		        } else { /* collision on right of block */
		        	e.setPositionX(blockX + centroidX);
		        	if(e.getVelocityX() < 0) {
						e.setVelocityX(0);
					}
		        }
		    } else {
		        if (wy > -hx) { /* collision on left of block, right of entity */
		        	e.setPositionX(blockX - centroidX);
		        	if(e.getVelocityX() > 0) {
						e.setVelocityX(0);
					}
		        } else { /* collision on top of block, bottom of entity */
		        	e.setPositionY(blockY + centroidY);
		        	onCollisionEntity(e, block.getFriction(), nanoseconds);
		        }
		    }
		}
	}

	private static void onCollisionEntity(Entity e, float surfaceFriction, long nanoseconds) {
		//friction force = normal force * friction
		//TODO maybe implement normal forces some time (for when entities are stacked)?
		//TODO must be independent of number of collisions, rely on Entity state (lastCollision)?
//		float friction = Utility.average(e.getFriction(), surfaceFriction);
//		long collisionTime = System.nanoTime();
//		long duration = Math.max(nanoseconds, collisionTime - e.lastCollision());
//    	if(e.getVelocityX() > 0) {
//    		//float left = Math.fma(-friction, duration, e.getVelocityX());
//    		float left = -friction / duration + e.getVelocityX();
//    		e.setVelocityX(Math.max(0, left));
//    	} else if(e.getVelocityX() < 0) {
//    		//float right = Math.fma(friction, duration, e.getVelocityX());
//    		float right = friction / duration + e.getVelocityX();
//    		e.setVelocityX(Math.min(0, right));
//    	}
//		e.setLastCollision(collisionTime);
		
		e.setVelocityY(0);
	}
}