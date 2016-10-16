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
		graphics.scale().setX(SIZE_X);
		graphics.scale().setY(SIZE_Y);
		graphics.setOpacity(0.75f);
		this.width = SIZE_X;
		this.height = SIZE_Y;
		this.blockTypes = blockTypes;
		this.cursorController = controller;
	}
	
	public Block getBlock() {
		return blockTypes[index];
	}

	@Override
	public void onPress() {
		graphics.setOpacity(1);
		pressed = true;
	}

	@Override
	public void onRelease() {
		if(index == blockTypes.length - 1) 
			index = 0;
		else index++;
		graphics.setModel(blockTypes[index].getModel());
		graphics.setOpacity(0.5f);
		cursorController.setBlock(blockTypes[index]);
		pressed = false;
	}

	@Override
	public void onHover() {
		hovered = true;
		graphics.scale().setX(ENLARGED_SIZE_X);
		graphics.scale().setY(ENLARGED_SIZE_Y);
		graphics.rotation().setRotation((float)Math.PI/4);
		width = ENLARGED_SIZE_X;
		height = ENLARGED_SIZE_Y;
	}

	@Override
	public void onUnHover() {
		hovered = false;
		graphics.scale().setX(SIZE_X);
		graphics.scale().setY(SIZE_Y);
		graphics.rotation().setRotation(0);
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
