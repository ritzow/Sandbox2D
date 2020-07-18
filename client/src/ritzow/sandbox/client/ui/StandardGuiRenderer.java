package ritzow.sandbox.client.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.util.Utility;

//TODO implement mouse and keyboard events.
//TODO add scissoring in order to display elements "inside" other elements, cropped.
//TODO make world renderer a GuiElement (will take quite a bit of work possibly)
//For mouse events, they will be consumed at the lowest level of recursion first (since those are drawn on top)
//and upper levels will not process them if consumed at the bottom.
//Will need a way to do intersection with basic button/element shapes like rectangles and circles, even when scaled and rotated (and both at same time).
//Input events will be processed entirely separately from drawing, but in a similar fashion using the same "offsets" system.
public class StandardGuiRenderer implements GuiRenderer {
	private final ModelRenderer program;
	private final Deque<GuiLevel> rt;
	private float mouseX, mouseY;
	private float scaleX, scaleY;
	private long nanos;

	private static class GuiLevel {
		private final RenderTransform transform;
		private final boolean cursorHover;

		public GuiLevel(RenderTransform transform, boolean cursorHover) {
			this.transform = transform;
			this.cursorHover = cursorHover;
		}
	}

	//Apply this Offset's transformation to the parameters (ie a left matrix multiply)
	private static record RenderTransform(float opacity, float x, float y, float scaleX, float scaleY, float rotation) {
		RenderTransform combine(float opacity, float x, float y, float scaleX, float scaleY, float rotation) {
			//https://en.wikipedia.org/wiki/Rotation_matrix
			//TODO deal with weird negatives required for clockwise/proper rotation
			float cos = (float)Math.cos(-this.rotation);
			float sin = (float)Math.sin(-this.rotation);
			return new RenderTransform(
				this.opacity() * opacity,
				(x * cos - y * sin) * this.scaleX + this.x,
				(x * sin + y * cos) * this.scaleY + this.y,
				this.scaleX * scaleX,
				this.scaleY * scaleY,
				this.rotation + rotation
				//this.scissor == null ? null : new Scissor(this.scaleX * this.scissor.width(), this.scaleY * this.scissor.height())
			);
		}
	}

	public StandardGuiRenderer(ModelRenderer modelProgram) {
		this.program = modelProgram;
		this.rt = new ArrayDeque<>();
		this.rt.addFirst(new GuiLevel(new RenderTransform(1, 0, 0, 1, 1, 0), true));
	}

	@Override
	public void draw(Model model, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		//TODO glScissor if exists
		//if exists program.flush() before and after, etc.
		RenderTransform transform = rt.peekFirst().transform.combine(opacity, posX, posY, scaleX, scaleY, rotation);
		program.queueRender(model, transform.opacity, transform.x, transform.y, transform.scaleX, transform.scaleY, transform.rotation);
		//TODO exit glScissor
	}

	@Override
	public void draw(GuiElement element, float posX, float posY) {
		draw(element, 1, posX, posY, 1, 1, 0);
	}

	@Override
	public void draw(GuiElement element, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		GuiLevel parent = rt.peekFirst();
		RenderTransform transform = parent.transform.combine(opacity, posX, posY, scaleX, scaleY, rotation);
		if(parent.cursorHover && intersect(element, transform)) {
			rt.addFirst(new GuiLevel(transform, true));
			element.render(this, nanos, 0, 0);
		} else {
			rt.addFirst(new GuiLevel(transform, false));
			element.render(this, nanos);
		}

		rt.removeFirst();
	}

	private boolean intersect(GuiElement element, RenderTransform transform) {
		//transform represents this gui element centered at the origin
		Shape shape = element.shape();
		if(shape instanceof Rectangle rect) {
			float cos = (float)Math.cos(-transform.rotation);
			float sin = (float)Math.sin(-transform.rotation);
			float mouseX = (this.mouseX * cos - this.mouseY * sin) * transform.scaleX + transform.x;
			float mouseY = (this.mouseX * sin + this.mouseY * cos) * transform.scaleY + transform.y;
			//TODO convert cursor coordinates into normalized coordinates within the Shape (rectangle in this case)
			boolean temp = Math.abs(mouseX) <= rect.width()/2 && Math.abs(mouseY) <= rect.height()/2;
			//System.out.println(mouseX + " " + mouseY + " " + rect.width()/2 + " " + rect.height()/2 + (temp ? "!!!" : ""));
			return temp;
		} else if(shape instanceof InfinitePlane) {
			return true;
		} else {
			throw new UnsupportedOperationException("unsupported Shape type");
		}
	}

	public void render(GuiElement gui, Framebuffer dest, Display display, long nanos, float guiScale) throws OpenGLException {
		dest.clear(161 / 256f, 252 / 256f, 156 / 256f, 1.0f);
		dest.setDraw();
		int windowWidth = display.width();
		int windowHeight = display.height();
		this.mouseX = Utility.convertRange(0, 1, -1, 1, (float)display.getCursorX()/windowWidth);
		this.mouseY = Utility.convertRange(0, 1, -1, 1, (float)display.getCursorY()/windowHeight);
		this.scaleX = guiScale/windowWidth;
		this.scaleY = guiScale/windowHeight;
		this.nanos = nanos;

		//load the view transformation
		//program.loadViewMatrixStandard(windowWidth, windowHeight);
		program.loadIdentityMatrix();

		//set the current shader program
		program.setCurrent();

		draw(gui, 1, 0, 0, scaleX, scaleY, 0);
	}
}
