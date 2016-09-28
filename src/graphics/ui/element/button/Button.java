package graphics.ui.element.button;

public interface Button {
	public void onPress();
	public void onRelease();
	public void onHover();
	public void onUnHover();
	public float getWidth();
	public float getHeight();
	public boolean getHovered();
	public boolean getPressed();
}