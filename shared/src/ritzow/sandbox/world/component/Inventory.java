package ritzow.sandbox.world.component;

import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.data.Serializer;
import ritzow.sandbox.data.Transportable;
import ritzow.sandbox.data.TransportableDataReader;
import ritzow.sandbox.world.item.Item;

public class Inventory<T extends Item> implements Transportable {
	protected final Item[] items;

	public Inventory(int capacity) {
		this.items = new Item[capacity];
	}

	public Inventory(TransportableDataReader input) {
		items = new Item[input.readInteger()];
		for(int i = 0; i < items.length; i++) {
			items[i] = input.readObject();
		}
	}

	@Override
	public byte[] getBytes(Serializer ser) {
		return Bytes.serializeArray(items, ser);
	}

	@Override
	public String toString() {
		StringBuilder list = new StringBuilder("Size: " + items.length + " [");
		for(int i = 0; i < items.length - 1; i++) {
			if(items[i] != null) {
				list.append(items[i]);
			}

			else {
				list.append("null");
			}

			list.append(", ");
		}

		if(items[items.length - 1] != null)
			list.append(items[items.length - 1]);
		else
			list.append("null");

		list.append(']');
		return list.toString();
	}

	@SuppressWarnings("unchecked")
	public T get(int slot) {
		return (T)items[slot];
	}

	public boolean isItem(int slot) {
		return items[slot] != null;
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

	@SuppressWarnings("unchecked")
	public T put(T item, int slot) {
		Object previous = items[slot];
		items[slot] = item;
		return (T)previous;
	}

	@SuppressWarnings("unchecked")
	public T remove(int slot) {
		Object item = items[slot];
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
