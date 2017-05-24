package ritzow.sandbox.world;

import ritzow.sandbox.audio.AudioSystem;
import ritzow.sandbox.util.Transportable;
import ritzow.sandbox.world.entity.Entity;

/**
 * Handler and organizer of {@link Entity} and {@link BlockGrid} objects. Handles updating of entities in the world and rendering of entities and blocks. 
 * Contains a foreground and background.
 */
public interface World extends Transportable, Iterable<Entity> {
	void add(Entity e);
	void remove(Entity e);
	void remove(int entityID);
	void update(float time);
	void setAudioSystem(AudioSystem audio);
	AudioSystem getAudioSystem();
	BlockGrid getForeground();
	BlockGrid getBackground();
}
