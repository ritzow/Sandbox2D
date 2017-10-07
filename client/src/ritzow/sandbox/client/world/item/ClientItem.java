package ritzow.sandbox.client.world.item;

import ritzow.sandbox.client.graphics.Graphics;
import ritzow.sandbox.world.Item;

/** Represents a game object that can be used by the player and stored in an inventory **/
public abstract class ClientItem extends Item {
	/**
	 * @return The graphics object representing the item's appearance
	 */
	public abstract Graphics getGraphics();
}
