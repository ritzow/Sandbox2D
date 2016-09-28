package graphics.ui.element.button;

import graphics.ui.element.GraphicsElement;
import world.block.Block;

public final class BlockSwitcherButton extends GraphicsElement implements Button {
	private boolean hovered;
	private boolean pressed;
	private float width;
	private float height;
	private int index;
	private Block[] blockTypes;
	
	private static final float SIZE_X = 0.25f;
	private static final float SIZE_Y = 0.25f;
	private static final float ENLARGED_SIZE_X = 0.35f;
	private static final float ENLARGED_SIZE_Y = 0.35f;
	
	public BlockSwitcherButton(Block[] blockTypes) {
		super(blockTypes[0].getModel(), 0, 0);
		graphics.scale().setX(SIZE_X);
		graphics.scale().setY(SIZE_Y);
		graphics.setOpacity(0.75f);
		this.width = SIZE_X;
		this.height = SIZE_Y;
		this.blockTypes = blockTypes;
	}

	@Override
	public void onPress() {
		pressed = true;
		if(index == blockTypes.length - 1) index = 0;
		else index++;
		graphics.setModel(blockTypes[index].getModel());
	}

	@Override
	public void onRelease() {
		pressed = false;
	}

	@Override
	public void onHover() {
		hovered = true;
		graphics.scale().setX(ENLARGED_SIZE_X);
		graphics.scale().setY(ENLARGED_SIZE_Y);
		graphics.setOpacity(1);
		width = ENLARGED_SIZE_X;
		height = ENLARGED_SIZE_Y;
	}

	@Override
	public void onUnHover() {
		hovered = false;
		graphics.scale().setX(SIZE_X);
		graphics.scale().setY(SIZE_Y);
		graphics.setOpacity(0.5f);
		width = SIZE_X;
		height = SIZE_Y;
	}

	@Override
	public float getWidth() {
		return width;
	}

	@Override
	public float getHeight() {
		return height;
	}

	@Override
	public boolean getHovered() {
		return hovered;
	}

	@Override
	public boolean getPressed() {
		return pressed;
	}

}
