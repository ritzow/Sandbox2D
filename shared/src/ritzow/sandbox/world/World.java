package ritzow.sandbox.world;

import static ritzow.sandbox.util.Utility.average;
import static ritzow.sandbox.util.Utility.intersection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
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

	/** collection of entities in the world **/
	private final List<Entity> entities; //TODO switch to using a Map<Integer, Entity> to store entity IDs?

	/** blocks in the world that collide with entities and and are rendered **/
	private final BlockGrid foreground, background;

	/** amount of downwards acceleration to apply to entities in the world **/
	private float gravity;

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
		entities = new ArrayList<>(100);
		foreground = new BlockGrid(width, height);
		background = new BlockGrid(width, height);
		this.gravity = gravity;
	}

	public World(TransportableDataReader reader) {
		gravity = reader.readFloat();
		foreground = Objects.requireNonNull(reader.readObject());
		background = Objects.requireNonNull(reader.readObject());
		int entityCount = reader.readInteger();
		entities = new ArrayList<>(entityCount);
		for(int i = 0; i < entityCount; i++) {
			entities.add(Objects.requireNonNull(reader.readObject()));
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

		for(int i = 0; i < numEntities; i++) {
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

		//write gravity
		Bytes.putFloat(bytes, index, gravity);
		index += 4;

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
		StringBuilder builder =
				new StringBuilder(foreground.toString()).append(background.toString());
		for(Entity e : entities) {
			builder.append(e).append('\n');
		}
		return builder.toString();
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
			if(predicate.test(it.next()))
				it.remove();
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

	/**
	 * Adds the provided non-null Entity to the world.
	 * @param e the entity to add.
	 */
	public final void add(Entity e) {
		Objects.requireNonNull(e);
		checkEntitiesModifiable();
		entities.add(e);
	}

	/**
	 * Removes the provided entity from the world.
	 * @param e the entity to remove.
	 */
	public final void remove(Entity e) {
		checkEntitiesModifiable();
		entities.remove(e);
	}

	public final float getGravity() {
		return gravity;
	}

	public final void setGravity(float gravity) {
		this.gravity = gravity;
	}

	/**
	 * Updates the entities in the world, simulating a single timestep of the provided amount.
	 * Entities are updated, gravity is applied, entity vs entity collisions are resolved,
	 * and entity vs block collisions are resolved. If {@code setRemoveEntities has been called},
	 * entities that are below the bottom of the world will be removed and, if provided, the entity
	 * remove handler will be called.
	 * @param time the amount of time to simulate.
	 */
	public final void update(float time) {
		isEntitiesModifiable = false;
		var entities = this.entities;
		int size = entities.size();
		for(int i = 0; i < size; i++) {
			Entity e = entities.get(i);

			//remove entities that are below the world or are flagged for deletion
			if(onRemove != null) {
				if(e.getPositionY() < 0 || e.getShouldDelete()) {
					onRemove.accept(entities.remove(i));
					i = Math.max(0, i - 1);
					size--;
					continue;
				}
			}

			//update entity position and velocity, and anything else specific to an entity
			e.update(this, time);

			//apply gravity
			e.setVelocityY(e.getVelocityY() - gravity * time);

			//check if collision checking is enabled
			boolean hasLogic = e.hasEntityCollisionLogic();
			boolean collidesEntities = e.collidesWithEntities();
			//check for entity vs. entity collisions with all entities that have not already been
			//collision checked with (for first element, all entites, for last, no entities)
			for(int j = i + 1; j < size; j++) {
				Entity o = entities.get(j);
				boolean otherHasLogic = o.hasEntityCollisionLogic();
				boolean isPhysicsCollision = collidesEntities && o.collidesWithEntities() && resolveCollision(e, o, time);
				boolean isCheckedCollision = (hasLogic || otherHasLogic) && (isPhysicsCollision || checkCollision(e, o));
				if(hasLogic && isCheckedCollision)
					e.onCollision(this, o, time);
				if(otherHasLogic && isCheckedCollision)
					o.onCollision(this, e, time);
			}

			//Check for entity collisions with blocks
			if(e.collidesWithBlocks()) {
				resolveBlockCollisions(e, time);
			}

			//again remove entities that are below the world or are flagged for deletion
			if(onRemove != null) {
				if(e.getPositionY() < 0 || e.getShouldDelete()) {
					onRemove.accept(entities.remove(i));
					i = Math.max(0, i - 1);
					size--;
				}
			}
		}
		isEntitiesModifiable = true;
	}

	private void resolveBlockCollisions(Entity e, float time) {
		int worldTop = foreground.getHeight()-1;
		int worldRight = foreground.getWidth()-1;
		int leftBound = Utility.clampLowerBound(0, e.getPositionX() - e.getWidth());
		int topBound = Utility.clampUpperBound(worldTop, e.getPositionY() + e.getHeight());
		int rightBound = Utility.clampUpperBound(worldRight, e.getPositionX() + e.getWidth());
		int bottomBound = Utility.clampLowerBound(0, e.getPositionY() - e.getHeight());

		for(int row = bottomBound; row <= topBound; row++) {
			for(int column = leftBound; column <= rightBound; column++) {
				Block block = foreground.get(column, row);
				if(foreground.isBlock(column, row) && block.isSolid()) {
					boolean blockUp = row == worldTop ? false : foreground.isBlock(column, row + 1);
					boolean blockDown = row == 0 ? false : foreground.isBlock(column, row - 1);
					boolean blockLeft = column == 0 ? false : foreground.isBlock(column - 1, row);
					boolean blockRight = column == worldRight ? false : foreground.isBlock(column + 1, row);

					if(!(blockUp && blockDown && blockLeft && blockRight)) {
						resolveBlockCollision(this, e, block, column, row, time, blockUp, blockLeft, blockRight, blockDown);
					}
				}
			}
		}
	}

	/**
	 * Checks if there is a collision between two entities
	 * @param e an entity
	 * @param o an entity
	 * @return true if the entities intersect eachother, false otherwise
	 */
	private final static boolean checkCollision(Entity e, Entity o) {
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
	private final static boolean checkCollision(Entity e, float otherX, float otherY, float otherWidth, float otherHeight) {
		 return (Math.abs(e.getPositionX() - otherX) * 2 < (e.getWidth() + otherWidth)) &&  (Math.abs(e.getPositionY() - otherY) * 2 < (e.getHeight() + otherHeight));
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
	private static boolean resolveCollision(Entity e, Entity o, float time) { //TODO use momentum (mass * velocity) to determine which one moves
		float width = 0.5f * (e.getWidth() + o.getWidth());
		float height = 0.5f * (e.getHeight() + o.getHeight());
		float deltaX = o.getPositionX() - e.getPositionX();
		float deltaY = o.getPositionY() - e.getPositionY();
		if (Math.abs(deltaX) < width && Math.abs(deltaY) < height) { /* collision! replace < in intersection detection with <= for previous behavior */
		    float wy = width * deltaY;
		    float hx = height * deltaX;
		    if (wy > hx) {
		        if (wy > -hx) { /* collision on the bottom of other */
		        	e.setPositionY(o.getPositionY() - height);
		        	collisionBottom(e, o.getFriction(), time);
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
		        	collisionTop(e, o.getFriction(), time);
		        }
		    }
		    return true;
		}
		return false;
	}

	private static boolean resolveBlockCollision(World world, Entity e, Block block, float blockX, float blockY, float time,
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
		        	collisionBottom(e, block.getFriction(), time);
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
		        	collisionTop(e, block.getFriction(), time);
		        }
		    }
			e.onCollision(world, block, Math.round(blockX), Math.round(blockY), time);
		    return true;
		}
		return false;
	}

	private static void collisionBottom(Entity e, float surfaceFriction, float time) {
		if(e.getVelocityY() > 0) {
			e.setVelocityY(0);
		} if(e.getVelocityX() > 0) {
			setVelocityEntityFrictionRight(e, surfaceFriction, time);
		} else if(e.getVelocityX() < 0) {
			setVelocityEntityFrictionLeft(e, surfaceFriction, time);
		}
	}

	private static void collisionTop(Entity e, float surfaceFriction, float time) {
    	if(e.getVelocityY() < 0) {
    		e.setVelocityY(0);
    	} if(e.getVelocityX() > 0) {
    		setVelocityEntityFrictionRight(e, surfaceFriction, time);
    	} else if(e.getVelocityX() < 0) {
    		setVelocityEntityFrictionLeft(e, surfaceFriction, time);
    	}
	}

	private static void setVelocityEntityFrictionRight(Entity e, float surfaceFriction, float time) {
		e.setVelocityX(Math.max(0, Math.fma(-average(e.getFriction(), surfaceFriction), time, e.getVelocityX())));
	}

	private static void setVelocityEntityFrictionLeft(Entity e, float surfaceFriction, float time) {
		e.setVelocityX(Math.min(0, Math.fma(average(e.getFriction(), surfaceFriction), time, e.getVelocityX())));
	}
}