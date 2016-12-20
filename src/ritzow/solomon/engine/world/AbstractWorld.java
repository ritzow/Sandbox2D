package ritzow.solomon.engine.world;

import java.util.ArrayList;
import java.util.List;
import ritzow.solomon.engine.graphics.Renderable;
import ritzow.solomon.engine.world.block.Block;
import ritzow.solomon.engine.world.entity.Entity;

public abstract class AbstractWorld implements Renderable {
	protected final Block[][] foreground, background;
	protected final List<Entity> entities;
	
	public AbstractWorld(int width, int height) {
		entities = new ArrayList<Entity>(100);
		foreground = new Block[height][width];
		background = new Block[height][width];
	}
	
	public abstract void update(float time);
	
	public Block getBlockForeground(float x, float y) {
		return foreground[Math.round(y)][Math.round(x)];
	}
	
	public Block getBlockBackground(float x, float y) {
		return background[Math.round(y)][Math.round(x)];
	}
	
	public void setBlockForeground(Block block, float x, float y) {
		foreground[Math.round(y)][Math.round(x)] = block;
	}
	
	public void setBlockBackground(Block block, float x, float y) {
		background[Math.round(y)][Math.round(x)] = block;
	}
	
	public float getWidth() {
		return foreground[0].length;
	}
	
	public float getHeight() {
		return foreground.length;
	}
	
	public synchronized void add(Entity e) {
		entities.add(e);
	}
	
	public synchronized void remove(Entity e) {
		entities.remove(e);
	}
}
