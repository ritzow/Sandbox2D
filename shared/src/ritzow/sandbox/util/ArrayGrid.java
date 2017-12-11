package ritzow.sandbox.util;

public class ArrayGrid<E> implements Grid<E> {
	private final Object[][] grid;
	
	public ArrayGrid(int rows, int columns) {
		grid = new Object[rows][columns];
	}

	@Override
	public void put(int row, int column, E element) {
		checkSize(row, column);
		grid[row][column] = element;
	}

	@SuppressWarnings("unchecked")
	@Override
	public E get(int row, int column) {
		checkSize(row, column);
		return (E)grid[row][column];
	}
	
	private final void checkSize(int row, int col) {
		if(width() <= col)
			throw new IllegalArgumentException("row exceeds grid size");
		if(height() <= row)
			throw new IllegalArgumentException("column exceeds grid size");
	}

	@Override
	public int width() {
		return grid[0].length;
	}

	@Override
	public int height() {
		return grid.length;
	}

}
