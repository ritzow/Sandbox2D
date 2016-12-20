package ritzow.solomon.engine.world.item;

import ritzow.solomon.engine.graphics.Graphics;
import ritzow.solomon.engine.util.Transportable;

/**
 * Represents a game object that can be held or used by the player
 * @author Solomon Ritzow
 *
 */
public abstract class Item implements Transportable {
	
	/**
	 * @return The graphics object representing the item's appearance
	 */
	public abstract Graphics getGraphics();
	
	/**
	 * @return a human-readable name that can be presented to the player
	 */
	public abstract String getName();
}
