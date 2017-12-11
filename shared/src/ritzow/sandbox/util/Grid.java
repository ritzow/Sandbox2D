package ritzow.sandbox.util;

public interface Grid<E> {
	public void put(int row, int column, E element);
	public E get(int row, int column);
	public int width();
	public int height();
}
