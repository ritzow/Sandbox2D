package ritzow.solomon.engine.world.base;

import ritzow.solomon.engine.audio.AudioSystem;
import ritzow.solomon.engine.graphics.Camera;
import ritzow.solomon.engine.graphics.Renderer;
import ritzow.solomon.engine.util.Transportable;
import ritzow.solomon.engine.world.entity.Entity;

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
	Renderer getRenderer(Camera camera);
}
