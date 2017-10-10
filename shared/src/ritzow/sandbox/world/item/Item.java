package ritzow.sandbox.world.item;

import ritzow.sandbox.data.Transportable;

/** Represents a game object that can be used by the player and stored in an inventory **/
public abstract class Item implements Transportable {
	
	/**
	 * Checks for equality with another item to see if the items can stack
	 * @param item the item to compare this item to
	 * @return whether or not the two items are the same
	 */
	public abstract boolean canStack(Item item);
	public abstract String getName();
	
	@Override
	public boolean equals(Object object) {
		return object instanceof Item && canStack((Item)object);
	}
}
