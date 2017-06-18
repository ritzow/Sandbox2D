package ritzow.sandbox.world;

import java.util.function.Consumer;
import ritzow.sandbox.audio.AudioSystem;
import ritzow.sandbox.util.Transportable;
import ritzow.sandbox.world.entity.Entity;

/**
 * Handler and organizer of {@link Entity} and {@link BlockGrid} objects. Handles updating of entities in the world. 
 * Contains a foreground and background.
 */
public interface World extends Transportable {
	void queueAdd(Entity e);
	void queueRemove(Entity e);
	void update(float time);
	void setAudioSystem(AudioSystem audio);
	void forEach(Consumer<Entity> consumer);
	int nextEntityID();
	Entity find(int entityID);
	AudioSystem getAudioSystem();
	BlockGrid getForeground();
	BlockGrid getBackground();
}
