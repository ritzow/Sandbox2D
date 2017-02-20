package ritzow.solomon.engine.ui;

public interface Button {
	public void onPress();
	public void onRelease();
	public void onHover();
	public void onUnHover();
	public boolean getHovered();
	public boolean getPressed();
}