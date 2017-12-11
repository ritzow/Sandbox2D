package ritzow.sandbox.util;

import java.util.ArrayList;
import java.util.List;

public class ArrayListGrid<E> implements Grid<E> {
	private final List<List<E>> grid;
	
	public ArrayListGrid(int rows, int columns) {
		this.grid = new ArrayList<List<E>>(rows);
		for(int i = 0; i < grid.size(); i++) {
			grid.set(i, new ArrayList<E>(columns));
		}
	}
	
//	public ExpandableGrid(DataReader reader) {
//		
//	}
//	
//	@Override
//	public byte[] getBytes(Serializer ser) {
//		byte[][] grid = new byte[this.grid.size()][];
//		for(int i = 0; i < this.grid.size(); i++) {
//			//TODO implement
//			//grid[i] = ByteUtil.serializeCollection(this.grid.get(i), ser);
//		}
//		byte[] data = ByteUtil.concatenate(8, grid);
//		ByteUtil.putInteger(data, 0, width());
//		ByteUtil.putInteger(data, 4, height());
//		return data;
//	}
	
	public int width() {
		return grid.size();
	}
	
	public int height() {
		return grid.get(0).size();
	}

	@Override
	public void put(int row, int column, E element) {
		grid.get(row).set(column, element);
	}

	@Override
	public E get(int row, int column) {
		return grid.get(row).get(column);
	}
}
