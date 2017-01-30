package ritzow.solomon.engine.world.item;

import ritzow.solomon.engine.graphics.Graphics;
import ritzow.solomon.engine.util.Transportable;

/**
 * Represents a game object that can be used by the player and stored in an inventory
 * @author Solomon Ritzow
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
	
	/**
	 * Checks for equality with another item to see if the items can stack
	 * @param item the item to compare this item to
	 * @return whether or not the two items are the same
	 */
	public abstract boolean equals(Item item);
	
}
