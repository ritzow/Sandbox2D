package ritzow.solomon.engine.world.base;

import ritzow.solomon.engine.audio.AudioSystem;
import ritzow.solomon.engine.graphics.Renderable;
import ritzow.solomon.engine.util.Transportable;
import ritzow.solomon.engine.world.entity.Entity;

/**
 * Handler and organizer of {@link Entity} and {@link BlockGrid} objects. Handles updating of entities in the world and rendering of entities and blocks. 
 * Contains a foreground and background.
 */
public abstract class World implements Renderable, Transportable, Iterable<Entity> {
	public abstract void add(Entity e);
	public abstract void remove(Entity e);
	public abstract void remove(int entityID);
	public abstract void update(float time);
	public abstract BlockGrid getForeground();
	public abstract BlockGrid getBackground();
	public abstract AudioSystem getAudioSystem();
}
