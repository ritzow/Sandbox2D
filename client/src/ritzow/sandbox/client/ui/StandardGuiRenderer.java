package ritzow.sandbox.client.ui;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.PriorityQueue;
import java.util.Queue;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.util.Utility;

//TODO investigate laggy rotations and stuff
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
	private final Queue<AnimationEvent> animations;
	private float mouseX, mouseY;
	private long nanos;

	private static final boolean DEBUG_PRINT_RENDER_TREE = true;

	private static record AnimationEvent(Animation animation, long start, long duration) implements Comparable<AnimationEvent> {
		@Override
		public int compareTo(AnimationEvent o) {
			return Long.compare(endTime(), o.endTime());
		}

		public long endTime() {
			return start + duration;
		}
	}

	private static class GuiLevel {
		private final RenderTransform transform;
		private boolean cursorHover;
		private final Rectangle bounds;

		public GuiLevel(RenderTransform transform, boolean cursorHover, Rectangle bounds) {
			this.transform = transform;
			this.cursorHover = cursorHover;
			this.bounds = bounds;
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

		Position transform(float x, float y) {
			float cos = (float)Math.cos(-rotation);
			float sin = (float)Math.sin(-rotation);
			return Position.of(
				(x * cos - y * sin) * this.scaleX + this.x,
				(x * sin + y * cos) * this.scaleY + this.y
			);
		}

		Position transformInverse(float x, float y) {
			float cos = (float)Math.cos(-rotation);
			float sin = (float)Math.sin(-rotation);
			x = (x - this.x) / this.scaleX;
			y = (y - this.y) / this.scaleY;
			return Position.of(
				(x * cos + y * sin),
				(x * -sin + y * cos)
			);
		}
	}

	public StandardGuiRenderer(ModelRenderer modelProgram) {
		this.program = modelProgram;
		this.rt = new ArrayDeque<>();
		this.animations = new PriorityQueue<>();
	}

	@Override
	public void draw(Model model, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		//TODO glScissor if exists
		//if exists program.flush() before and after, etc.
		RenderTransform transform = rt.peekFirst().transform.combine(opacity, posX, posY, scaleX, scaleY, rotation);
		program.queueRender(
			model,
			transform.opacity,
			transform.x,
			transform.y,
			transform.scaleX,
			transform.scaleY,
			transform.rotation
		);
		//TODO exit glScissor
	}

	@Override
	public void draw(GuiElement element, float posX, float posY) {
		draw(element, 1, posX, posY, 1, 1, 0);
	}

	private Rectangle parentBounds;

	@Override
	public Rectangle parent() {
		return parentBounds;
	}

	@Override
	public void draw(GuiElement element, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		if(StandardClientOptions.DEBUG && DEBUG_PRINT_RENDER_TREE) {
			System.out.println("  ".repeat(rt.size()) + element);
		}
		GuiLevel parent = rt.peekFirst();
		parentBounds = parent.bounds;
		RenderTransform transform = parent.transform.combine(opacity, posX, posY, scaleX, scaleY, rotation);
		Rectangle bounds = element.shape(parent.bounds).toRectangle(); //TODO if parent is dynamically sized by children, shape() should be called instead
		if(parent.cursorHover && intersect(element, parent.bounds, transform)) {
			rt.addFirst(new GuiLevel(transform, true, bounds));
			Position pos = transform.transformInverse(mouseX, mouseY);
			//TODO set cursorHover to false if render with cursor returns true
			parent.cursorHover  = element.render(this, nanos, pos.x(), pos.y());
			//TODO implement event consumption (ie prevent consumed events from being propogated further)
			//TODO mouse event processing must happen after render, so that lower elements dont received consumed event, maybe that just means encouraging proper draw
			//and update order?
		} else {
			rt.addFirst(new GuiLevel(transform, false, bounds));
			element.render(this, nanos);
		}

		rt.removeFirst();
	}

	private boolean intersect(GuiElement element, Rectangle parent, RenderTransform transform) {
		//transform represents this gui element centered at the origin
		Shape shape = element.shape(parent);
		if(shape instanceof Rectangle rect) {
			//Transform the rectangle back to origin (basically just the rectangle width and height)
			//and I transform (translate, scale, rotate) the mouse back to origin based on how the rectangle was transformed
			//basically, I perform the inverse of 'transform' to the mouse position then check bounds.
			Position mouseInverse = transform.transformInverse(mouseX, mouseY);
			if(StandardClientOptions.DEBUG) {
				program.queueRender(GameModels.MODEL_DIRT_BLOCK, 1, mouseInverse.x(), mouseInverse.y(), 0.05f,0.05f, 0);
				program.queueRender(GameModels.MODEL_GREEN_FACE, 1, 0, 0, rect.width(), rect.height(), 0);
			}
			return Math.abs(mouseInverse.x()) <= rect.width()/2 && Math.abs(mouseInverse.y()) <= rect.height()/2;
		} else if(shape instanceof Circle c) {
			Position mouseInverse = transform.transformInverse(mouseX, mouseY);
			return Utility.withinDistance(mouseInverse.x(), mouseInverse.y(), 0, 0, c.radius());
		} else if(shape instanceof Position) {
			return false;
		} else {
			throw new UnsupportedOperationException("unsupported Shape type");
		}
	}

	@Override
	public void play(Animation animation, Duration duration) {
		animations.add(new AnimationEvent(animation, System.nanoTime(), duration.toNanos()));
	}

	//TODO have this return whether a "UI user action" was consumed so it is known whether to apply it to other UI-like things such as the game world
	public void render(GuiElement gui, Framebuffer dest, Display display, long nanos, float guiScale) throws OpenGLException {
		if(!animations.isEmpty()) {
			long currentTime = System.nanoTime();
			while(!animations.isEmpty() && animations.peek().endTime() < currentTime) {
				animations.poll().animation.onEnd();
			}

			for(AnimationEvent event : animations) {
				long progressNanos = currentTime - event.start;
				event.animation().update(progressNanos, (float)((double)progressNanos/event.duration), nanos);
			}
		}


		dest.clear(161 / 256f, 252 / 256f, 156 / 256f, 1.0f);
		dest.setDraw();
		int windowWidth = display.width();
		int windowHeight = display.height();

		//convert from pixel coords (from top left) to UI coords
		float scaleX = guiScale / windowWidth;
		this.mouseX = Utility.convertRange(0, windowWidth, -1/ scaleX, 1/ scaleX, display.getCursorX());
		float scaleY = guiScale / windowHeight;
		this.mouseY = -Utility.convertRange(0, windowHeight, -1/ scaleY, 1/ scaleY, display.getCursorY());

		//boolean leftMouse = display.isControlActivated(Control.UI_ACTIVATE);
		//TODO should wrap mouseX, mouseY, and left/right/middle mouse into single "UI user action" event

		this.nanos = nanos;
		program.loadViewMatrixScale(guiScale, windowWidth, windowHeight);
		program.setCurrent();
		this.rt.addFirst(new GuiLevel(new RenderTransform(1, 0, 0, 1, 1, 0), true, new Rectangle(2/scaleX, 2/scaleY)));
		draw(gui, 1, 0, 0, 1, 1, 0);
		this.rt.removeFirst();
	}
}
