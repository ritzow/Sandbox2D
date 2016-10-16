package graphics.ui.element.button;

import graphics.ui.element.GraphicsElement;
import input.controller.CursorController;
import world.block.Block;

public final class BlockSwitcherButton extends GraphicsElement implements Button {
	private boolean hovered;
	private boolean pressed;
	private float width;
	private float height;
	private int index;
	private Block[] blockTypes;
	private CursorController cursorController;
	
	private static final float SIZE_X = 0.25f;
	private static final float SIZE_Y = 0.25f;
	private static final float ENLARGED_SIZE_X = 0.3f;
	private static final float ENLARGED_SIZE_Y = 0.3f;
	
	public BlockSwitcherButton(Block[] blockTypes, CursorController controller) {
		super(blockTypes[0].getModel(), 0, 0);
		graphics.getScale().setX(SIZE_X);
		graphics.getScale().setY(SIZE_Y);
		graphics.setOpacity(0.75f);
		this.width = SIZE_X;
		this.height = SIZE_Y;
		this.blockTypes = blockTypes;
		this.cursorController = controller;
		cursorController.setBlock(blockTypes[index]);
	}
	
	public Block getBlock() {
		return blockTypes[index];
	}

	@Override
	public void onPress() {
		graphics.getScale().setX(ENLARGED_SIZE_X);
		graphics.getScale().setY(ENLARGED_SIZE_Y);
		pressed = true;
	}

	@Override
	public void onRelease() {
		graphics.getScale().setX(SIZE_X);
		graphics.getScale().setY(SIZE_Y);
		if(index == blockTypes.length - 1) 
			index = 0;
		else index++;
		graphics.setModel(blockTypes[index].getModel());
		cursorController.setBlock(blockTypes[index]);
		pressed = false;
	}

	@Override
	public void onHover() {
		hovered = true;
		graphics.setOpacity(1);
		width = ENLARGED_SIZE_X;
		height = ENLARGED_SIZE_Y;
	}

	@Override
	public void onUnHover() {
		hovered = false;
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
