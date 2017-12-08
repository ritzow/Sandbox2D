package ritzow.sandbox.util;

public interface Grid<E> {
	public void put(int row, int column, E element);
	public void get(int row, int column);
}
