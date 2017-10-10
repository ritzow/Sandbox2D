package ritzow.sandbox.world.component;

import ritzow.sandbox.data.ByteUtil;
import ritzow.sandbox.data.DataReader;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.data.Transportable;
import ritzow.sandbox.world.item.Item;

@SuppressWarnings("unchecked")
public class Inventory<T extends Item> implements Transportable {
	protected final Item[] items;
	
	public Inventory(int capacity) {
		this.items = new Item[capacity];
	}
	
	public Inventory(DataReader input) {
		items = new Item[input.readInteger()];
		for(int i = 0; i < items.length; i++) {
			items[i] = input.readObject();
		}
	}
	
	@Override
	public byte[] getBytes(Serializer ser) {
		return ByteUtil.serializeArray(items, ser);
		
//		byte[] numItems = new byte[4];
//		ByteUtil.putInteger(numItems, 0, items.length);
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		try {
//			out.write(numItems);
//			
//			for(Item i : items) {
//				out.write(ser.serialize(i));
//			}
//			
//		} catch (IOException e) {
//			return null;
//		}
//		return out.toByteArray();
	}
	
	@Override
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
	
	public T get(int slot) {
		return (T)items[slot];
	}
	
	public int getSize() {
		return items.length;
	}
	
	public boolean add(T item) {
		for(int i = 0; i < items.length; i++) {
			if(items[i] == null) {
				items[i] = item;
				return true;
			}
		}
		return false;
	}
	
	public T put(Item item, int slot) {
		Item previous = items[slot];
		items[slot] = item;
		return (T)previous;
	}
	
	public T remove(int slot) {
		Item item = items[slot];
		items[slot] = null;
		return (T)item;
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
