package ritzow.sandbox.client.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.ui.element.GuiElement;

//TODO implement mouse and keyboard events.
//For mouse events, they will be consumed at the lowest level of recursion first (since those are drawn on top)
//and upper levels will not process them if consumed at the bottom.
//Will need a way to do intersection with basic button/element shapes like rectangles and circles, even when scaled and rotated (and both at same time).
//Input events will be processed entirely separately from drawing, but in a similar fashion using the same "offsets" system.
public class StandardGuiRenderer implements GuiRenderer {
	private final ModelRenderer program;
	private final Deque<Offset> offsets;

	//Apply this Offset's transformation to the parameters (ie a left matrix multiply)
	private static record Offset(float opacity, float x, float y, float scaleX, float scaleY, float rotation) {
		Offset combine(float opacity, float x, float y, float scaleX, float scaleY, float rotation) {
			//https://en.wikipedia.org/wiki/Rotation_matrix
			//TODO deal with weird negatives required for clockwise/proper rotation
			//x' = xcos(angle) - ysin(angle)
			//y' = xsin(angle) - ycos(angle)
			float cos = (float)Math.cos(-this.rotation);
			float sin = (float)Math.sin(-this.rotation);
			//x = x * this.scaleX;
			//y = y * this.scaleY;
			float newX = x * cos - y * sin;
			float newY = x * sin + y * cos;

			return new Offset(
				this.opacity() * opacity,
				newX * this.scaleX + this.x,
				newY * this.scaleY + this.y,
				this.scaleX * scaleX,
				this.scaleY * scaleY,
				this.rotation + rotation
			);
		}
	}

	public StandardGuiRenderer(ModelRenderer modelProgram) {
		this.program = modelProgram;
		this.offsets = new ArrayDeque<>();
		this.offsets.addFirst(new Offset(1, 0, 0, 1, 1, 0));
	}

	@Override
	public void draw(Model model, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		Offset offset = offsets.peekFirst().combine(opacity, posX, posY, scaleX, scaleY, rotation);
		program.queueRender(model, offset.opacity, offset.x, offset.y, offset.scaleX, offset.scaleY, offset.rotation);
	}

	@Override
	public void draw(GuiElement element, float posX, float posY) {
		draw(element, 1, posX, posY, 1, 1, 0);
	}

	@Override
	public void draw(GuiElement element, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		offsets.addFirst(offsets.peekFirst().combine(opacity, posX, posY, scaleX, scaleY, rotation));
		element.render(this);
		offsets.removeFirst();
	}

	public void render(GuiElement gui, Framebuffer dest, int framebufferWidth, int framebufferHeight, float scale) throws OpenGLException {
		dest.clear(161 / 256f, 252 / 256f, 156 / 256f, 1.0f);
		dest.setDraw();

		//load the view transformation
		program.loadViewMatrixStandard(framebufferWidth, framebufferHeight);

		//set the current shader program
		program.setCurrent();

		draw(gui, 1, 0, 0, scale, scale, 0);
	}
}
