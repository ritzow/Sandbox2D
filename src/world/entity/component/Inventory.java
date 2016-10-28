package world.entity.component;

import world.item.Item;

public class Inventory {
	
	protected Item[] inventory;
	
	public Inventory(int capacity) {
		this.inventory = new Item[capacity];
	}
	
	public Item get(int slot) {
		return inventory[slot];
	}
	
	public int getSize() {
		return inventory.length;
	}
	
	public boolean add(Item item) {
		for(int i = 0; i < inventory.length; i++) {
			if(inventory[i] == null) {
				inventory[i] = item;
				return true;
			}
		}
		return false;
	}
	
	public Item put(Item item, int slot) {
		Item previous = inventory[slot];
		inventory[slot] = item;
		return previous;
	}
	
	public Item remove(int slot) {
		Item item = inventory[slot];
		inventory[slot] = null;
		return item;
	}
	
	public void condense() {
		for(int i = 0; i < inventory.length; i++) {
			if(inventory[i] != null) {
				for(int j = 0; j < inventory.length; j++) {
					if(inventory[j] == null) {
						inventory[j] = inventory[i];
						inventory[i] = null;
						break;
					}
				}
			}
		}
	}
}
