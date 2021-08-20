package ritzow.sandbox.world;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.data.Transportable;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.block.Block;
import ritzow.sandbox.world.entity.Entity;

import static ritzow.sandbox.util.Utility.intersection;

/**
 * A World instance manages a foreground and background BlockGrid, and a collection of entities.
 * @author Solomon Ritzow
 *
 */
public class World implements Transportable, Iterable<Entity> {

	private static final float GRAVITY = Utility.convertAccelerationSecondsNanos(9.8f * 3);

	public static final int LAYER_MAIN = 0, LAYER_BACKGROUND = 1;

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
	private final BlockGrid blocks;

	/** Entity ID counter **/
	private int lastEntityID;

	/** called when an entity is removed from the world, entities won't be removed if null **/
	private Consumer<Entity> onRemove;

	/** For access protection during entity updates **/
	private boolean isEntitiesUnmodifiable = false;

	private final RandomGenerator randgen = RandomGeneratorFactory.of("L64X128MixRandom").create();

	/**
	 * Initializes a new World object with a foreground, background, entity storage, and gravity.
	 * @param width the width of the foreground and background
	 * @param height the height of the foreground and background
	 */
	public World(int width, int height) {
		entities = new ArrayList<>();
		entitiesID = new HashMap<>();
		blocks = new BlockGrid(2, width, height);
	}

	public World(TransportableDataReader reader) {
		blocks = Objects.requireNonNull(reader.readObject(), "block grid can't be null.");
		int entityCount = reader.readInteger();
		entities = new ArrayList<>(entityCount);
		entitiesID = new HashMap<>(entityCount);
		int maxEntityID = 0;
		for(int i = 0; i < entityCount; ++i) {
			Entity e = Objects.requireNonNull(reader.readObject(), "null entities prohibited by World");
			int id = e.getID();
			if(id > maxEntityID) maxEntityID = id;
			addEntity(e);
		}
		lastEntityID = maxEntityID;
	}

	public RandomGenerator random() {
		return randgen;
	}

	@Override
	public final byte[] getBytes(Serializer ser) { //needed for saving world to file as Transportable
		return getBytesFiltered(e -> true, ser);
	}

	public final byte[] getBytesFiltered(Predicate<Entity> entityFilter, Serializer ser) {
		//serialize foreground and background
		byte[] blocksBytes = ser.serialize(blocks);

		List<byte[]> elist = new ArrayList<>();
		int totalEntityBytes = entities.stream()
				.filter(entityFilter)
				.map(ser::serialize)
				.peek(elist::add)
				.mapToInt(a -> a.length)
				.sum();

		//foreground data, background data, number of entities, entity data (size)
		byte[] bytes = new byte[blocksBytes.length + Integer.BYTES + totalEntityBytes];
		int index = 0;

		//write foreground data
		Bytes.copy(blocksBytes, bytes, index);
		index += blocksBytes.length;

		//write number of entities
		Bytes.putInteger(bytes, index, elist.size());
		index += Integer.BYTES;

		for(byte[] dat : elist) {
			Bytes.copy(dat, bytes, index);
			index += dat.length;
		}
		return bytes;
	}

	@Override
	public String toString() {
		return new StringJoiner("\n", "Entity Count: ", "")
			.add(Integer.toString(entities.size()))
			.add(blocks.toString())
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
	 * @return The foreground terrain.
	 */
	public final BlockGrid getBlocks() {
		return blocks;
	}

	/**
	 * Enables entity removal and provides an action to take when the world removes entities when updated
	 * @param onRemove action to take when an entity is removed
	 */
	public void setRemoveEntities(Consumer<Entity> onRemove) {
		this.onRemove = Objects.requireNonNull(onRemove);
	}

	/**
	 * Enables entity removal.
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
				entitiesID.remove(next.getID());
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

	public Entity getEntityFromIdOrNull(int id) {
		return entitiesID.get(id);
	}

	public Entity getEntityFromID(int id) {
		return Objects.requireNonNull(entitiesID.get(id), "No entity in world with provided ID");
	}

	/**
	 * Returns a collection of entities that are partially or fully within the given rectangle bounds
	 * @param x the center x coordinate
	 * @param y the center y coordinate
	 * @param width the width of the rectangle
	 * @param height the height of the rectangle
	 * @return A collection of all the entities in the defined rectangle.
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
		if(isEntitiesUnmodifiable)
			throw new IllegalStateException("cannot add/remove from world: world is being updated");
	}

	private void addEntity(Entity e) {
		entities.add(e);
		entitiesID.put(e.getID(), e);
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
		remove(e.getID());
	}

	/** Removes the entity with id {@code entityID} from the world,
	 * returns null if the entity does not exist. */
	public final Entity remove(int entityID) {
		Entity e = entitiesID.remove(entityID); //Objects.requireNonNull(, "entity not found");
		if(!entities.remove(e))
			throw new IllegalStateException("entity not found in list, but found in map");
		return e;
	}

	private static final float FRICTION_COEFFICIENT = 100_000_000_000_000_000f;

	/**
	 * Updates the entities in the world, simulating a single timestep of the provided amount.
	 * Entities are updated, gravity is applied, entity vs entity collisions are resolved,
	 * and entity vs block collisions are resolved. If {@code setRemoveEntities has been called},
	 * entities that are below the bottom of the world will be removed and, if provided, the entity
	 * remove handler will be called.
	 * @param nanoseconds the amount of time to simulate.
	 */
	public final void update(long nanoseconds) {
		isEntitiesUnmodifiable = true;
		var entities = this.entities;
		int size = entities.size();
		for(int i = 0; i < size; i++) {
			Entity e = entities.get(i);
			//remove entities that are below the world or are flagged for deletion
			if(onRemove != null && e.getPositionY() < 0 || e.getShouldDelete()) {
				entitiesID.remove(e.getID());
				onRemove.accept(entities.remove(i));
				size--;
			} else {
				//update anything specific to an entity, can update position and velocity
				e.update(this, nanoseconds);

				//update position and velocity
				e.setPositionX(Math.fma(e.getVelocityX(), nanoseconds, e.getPositionX()));
				e.setVelocityY(Math.fma(-GRAVITY, nanoseconds, e.getVelocityY()));
				e.setPositionY(Math.fma(e.getVelocityY(), nanoseconds, e.getPositionY()));

				//check for entity vs. entity collisions with all entities that have not already been
				//collision checked with (for first element, all entites, for last, no entities)
				for(int j = i + 1; j < size; j++) {
					resolveEntityCollision(e, entities.get(j), nanoseconds);
				}

				//Check for entity collisions with blocks
				if(e.collidesWithBlocks()) {
					float friction = resolveBlockCollisions(e, nanoseconds);
					if(friction != 0) {
						friction = Utility.average(e.getFriction(), friction);
						float delta = nanoseconds / friction / FRICTION_COEFFICIENT;
						if(e.getVelocityX() > 0) {
							e.setVelocityX(Math.max(0, e.getVelocityX() - delta));
						} else if(e.getVelocityX() < 0) {
							e.setVelocityX(Math.min(e.getVelocityX() + delta, 0));
						}
					}
				}
			}
		}
		isEntitiesUnmodifiable = false;
	}

	private float resolveBlockCollisions(Entity e, long nanoseconds) {
		BlockGrid blocks = this.blocks;
		float posX = e.getPositionX();
		float posY = e.getPositionY();
		float width = e.getWidth();
		float height = e.getHeight();
		int leftBound = Math.max(0, (int)(posX - width));
		int bottomBound = Math.max(0, (int)(posY - height));
		int topBound = Math.min((int)(posY + height), blocks.getHeight() - 1);
		int rightBound = Math.min((int)(posX + width), blocks.getWidth() - 1);
		float friction = 0;
		int frictionCollisions = 0;

		//TODO is there redundancy between this and resolveBlockCollision
		//System.out.println(leftBound + ", " + bottomBound + ", " + rightBound + ", " + topBound);
		for(int row = bottomBound; row <= topBound; row++) {
			for(int column = leftBound; column <= rightBound; column++) {
				Block block = blocks.get(LAYER_MAIN, column, row);
				if(block != null && block.isSolid()) {
					//TODO re-add other block checks to see if surface is smooth
					float f = resolveBlockCollision(e, block, column, row, nanoseconds);
					if(f != 0) {
						friction += f;
						frictionCollisions++;
					}
				}
			}
		}
		return frictionCollisions > 0 ? friction / frictionCollisions : 0; //average of frictions
	}

	/**
	 * Resolves a collision between an entity and a hitbox.
	 * The entity passed into the first parameter will be moved if necessary.
	 * @param e the entity to be resolved
	 * @param o the entity to check collision with
	 * @param nanoseconds the amount of time that the resolution should simulate
	 */
	private void resolveEntityCollision(Entity e, Entity o, long nanoseconds) {
		if(e.interactsWithEntities() || o.interactsWithEntities()) {
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
					        e.setVelocityY(0);
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
					        e.setVelocityY(0);
				        }
				    }
				}
			}
		}
	}

	private boolean isOpen(int x, int y) {
		return !(blocks.isValid(LAYER_MAIN, x, y) && blocks.isBlock(LAYER_MAIN, x, y) && blocks.get(LAYER_MAIN, x, y).isSolid());
	}

	/** Returns true if entity is grounded **/
	private float resolveBlockCollision(Entity e, Block block, int blockX, int blockY, long nanoseconds) {
		float centroidX = Math.fma(0.5f, e.getWidth(), 0.5f);
		float centroidY = Math.fma(0.5f, e.getHeight(), 0.5f);
		float deltaX = blockX - e.getPositionX();
		float deltaY = blockY - e.getPositionY();
		if (Math.abs(deltaX) < centroidX && Math.abs(deltaY) < centroidY) { /* collision! */
		    float wy = centroidX * deltaY;
		    float hx = centroidY * deltaX;
		    if (wy > hx) {
		        if (wy > -hx && isOpen(blockX, blockY - 1)) { /* collision on the bottom of block, top of entity */
		        	e.setPositionY(blockY - centroidY);
					e.onCollision(this, block, Entity.Side.TOP, blockX, blockY, nanoseconds);
					if(e.getVelocityY() > 0) {
						e.setVelocityY(0);
					}
		        } else if(isOpen(blockX + 1, blockY)) { /* collision on right of block */
		        	e.setPositionX(blockX + centroidX);
		        	e.onCollision(this, block, Entity.Side.LEFT, blockX, blockY, nanoseconds);
		        	if(e.getVelocityX() < 0) {
						e.setVelocityX(0);
					}
		        }
		    } else {
		        if (wy > -hx && isOpen(blockX - 1, blockY)) { /* collision on left of block, right of entity */
		        	e.setPositionX(blockX - centroidX);
		        	e.onCollision(this, block, Entity.Side.RIGHT, blockX, blockY, nanoseconds);
		        	if(e.getVelocityX() > 0) {
						e.setVelocityX(0);
					}
		        } else if(isOpen(blockX, blockY + 1)) { /* collision on top of block, bottom of entity */
		        	e.setPositionY(blockY + centroidY);
		        	e.onCollision(this, block, Entity.Side.BOTTOM, blockX, blockY, nanoseconds);
		        	//updateFriction(e, block.getFriction(), 0.0000001f, nanoseconds);
			        if(e.getVelocityY() < 0) {
				        e.setVelocityY(0);
			        }
			        return block.getFriction();
		        }
		    }
		}
		return 0;
	}

}