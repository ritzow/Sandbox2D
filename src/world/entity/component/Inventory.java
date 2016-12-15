package world.entity.component;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import world.item.Item;

public class Inventory implements Externalizable {
	
	protected Item[] items;
	
	public Inventory(int capacity) {
		this.items = new Item[capacity];
	}
	
	public Inventory() {/* for serialization */}
	
	public String toString() {
		StringBuilder list = new StringBuilder("Size: " + items.length + " [");
		for(int i = 0; i < items.length - 1; i++) {
			if(items[i] != null) {
				list.append(items[i].getName());
			}
			
			else {
				list.append("null");
			}
			
			list.append(", ");
		}
		
		if(items[items.length - 1] != null)
			list.append(items[items.length - 1].getName());
		else
			list.append("null");
		
		list.append(']');
		return list.toString();
	}
	
	public Item get(int slot) {
		return items[slot];
	}
	
	public int getSize() {
		return items.length;
	}
	
	public boolean add(Item item) {
		for(int i = 0; i < items.length; i++) {
			if(items[i] == null) {
				items[i] = item;
				return true;
			}
		}
		return false;
	}
	
	public Item put(Item item, int slot) {
		Item previous = items[slot];
		items[slot] = item;
		return previous;
	}
	
	public Item remove(int slot) {
		Item item = items[slot];
		items[slot] = null;
		return item;
	}
	
	public void condense() {
		for(int i = 0; i < items.length; i++) {
			if(items[i] != null) {
				for(int j = 0; j < items.length; j++) {
					if(items[j] == null) {
						items[j] = items[i];
						items[i] = null;
						break;
					}
				}
			}
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(items);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.items = (Item[])in.readObject();
	}
}
