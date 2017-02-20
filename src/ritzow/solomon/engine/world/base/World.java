package ritzow.solomon.engine.world.base;

import ritzow.solomon.engine.graphics.Renderable;
import ritzow.solomon.engine.util.Transportable;
import ritzow.solomon.engine.world.entity.Entity;

public abstract class World implements Renderable, Transportable, Iterable<Entity> {
	public abstract void add(Entity e);
	public abstract void remove(Entity e);
	public abstract void update(float time);
	public abstract BlockGrid getForeground();
	public abstract BlockGrid getBackground();
	public abstract float getGravity();
}
