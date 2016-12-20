package ritzow.solomon.engine.world.entity.component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.util.Transportable;
import ritzow.solomon.engine.world.item.Item;

public class Inventory implements Transportable {
	
	protected final Item[] items;
	
	public Inventory(int capacity) {
		this.items = new Item[capacity];
	}
	
	public Inventory(byte[] data) throws ReflectiveOperationException {
		items = new Item[ByteUtil.getInteger(data, 0)];
		int index = 4;
		for(int i = 0; i < items.length; i++) {
			items[i] = (Item)ByteUtil.deserialize(data, index);
			index += ByteUtil.getSerializedLength(data, index);
		}
	}
	
	@Override
	public byte[] getBytes() {
		byte[] numItems = new byte[4];
		ByteUtil.putInteger(numItems, 0, items.length);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			out.write(numItems);
			
			for(Item i : items) {
				out.write(ByteUtil.serialize(i));
			}
			
		} catch (IOException e) {
			return null;
		}
		return out.toByteArray();
	}
	
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
}
