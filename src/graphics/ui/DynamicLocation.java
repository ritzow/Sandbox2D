package graphics.ui;

public class DynamicLocation {
	
	public float horizontal;
	public float vertical;
	public float paddingX;
	public float paddingY;
	
	/**
	 * @param horizontal the horizontal position of the element on the screen, a number between -1 and 1
	 * @param vertical the vertical position of the element on the screen, a number between -1 and 1
	 * @param paddingX the horizontal position offset of the element from the edge of the screen
	 * @param paddingY the vertical position offset of the element from the edge of the screen
	 */
	public DynamicLocation(float horizontal, float vertical, float paddingX, float paddingY) {
		this.horizontal = horizontal;
		this.vertical = vertical;
		this.paddingX = paddingX;
		this.paddingY = paddingY;
	}
	
}
