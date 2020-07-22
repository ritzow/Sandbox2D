package ritzow.sandbox.client.ui;

import java.time.Duration;
import java.util.*;
import ritzow.sandbox.client.data.StandardClientOptions;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.input.ControlsQuery;
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
	private final Deque<GuiLevel> rt; //TODO compress this by adding a boolean or int field indicating the child has the exact same properties
	private final Queue<AnimationEvent> animations;

	private ControlsQuery controls;
	private Rectangle parentBounds;
	private Optional<Position> mousePos;
	private float mouseX, mouseY; //TODO these might need to be in a stack
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
			if(x == 0.0f && y == 0.0f && scaleX == 1.0f && scaleY == 1.0f && rotation == 0.0f && opacity == 1.0f) {
				return this;
			} else if(this.rotation == 0.0f) {
				return new RenderTransform(
					this.opacity * opacity,
					x * this.scaleX + this.x,
					y * this.scaleY + this.y,
					this.scaleX * scaleX,
					this.scaleY * scaleY,
					rotation
				);
			} else {
				//https://en.wikipedia.org/wiki/Rotation_matrix
				//TODO deal with weird negatives required for clockwise/proper rotation
				float cos = (float)Math.cos(-this.rotation);
				float sin = (float)Math.sin(-this.rotation);
				return new RenderTransform(
					this.opacity * opacity,
					(x * cos - y * sin) * this.scaleX + this.x,
					(x * sin + y * cos) * this.scaleY + this.y,
					this.scaleX * scaleX,
					this.scaleY * scaleY,
					this.rotation + rotation
					//this.scissor == null ? null : new Scissor(this.scaleX * this.scissor.width(), this.scaleY * this.scissor.height())
				);
			}
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
	public Rectangle parent() {
		return parentBounds;
	}

	@Override
	public ControlsQuery controls() {
		return controls;
	}

	@Override
	public Optional<Position> mousePos() {
		return mousePos;
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
	public void draw(GuiElement element) {
		drawInternal(element, rt.peekFirst().transform);
	}

	@Override
	public void draw(GuiElement element, float posX, float posY) {
		draw(element, 1, posX, posY, 1, 1, 0);
	}

	//TODO add draw(GuiElement) which copies RenderTransform from parent

	@Override
	public void draw(GuiElement element, float opacity, float posX, float posY, float scaleX, float scaleY) {
		drawInternal(element, rt.peekFirst().transform.combine(opacity, posX, posY, scaleX, scaleY, 0));
	}

	@Override
	public void draw(GuiElement element, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		drawInternal(element, rt.peekFirst().transform.combine(opacity, posX, posY, scaleX, scaleY, rotation));
	}

	private void drawInternal(GuiElement element, RenderTransform transform) {
		if(StandardClientOptions.DEBUG && DEBUG_PRINT_RENDER_TREE) {
			System.out.println("  ".repeat(rt.size()) + element);
		}
		GuiLevel parent = rt.peekFirst();
		parentBounds = parent.bounds;
		//TODO if parent is dynamically sized by children, shape() should be called instead
		Rectangle bounds = element.shape(parent.bounds).toRectangle();
		//transform mouse position into GUI element's coordinate system.
		Position mouseInverse = transform.transformInverse(mouseX, mouseY);
		if(StandardClientOptions.DEBUG) {
			Rectangle rect = element.shape(parent.bounds).toRectangle();
			program.queueRender(GameModels.MODEL_DIRT_BLOCK, 1, mouseInverse.x(), mouseInverse.y(), 0.05f,0.05f, 0);
			program.queueRender(GameModels.MODEL_GREEN_FACE, 1, 0, 0, rect.width(), rect.height(), 0);
		}

		//TODO I can remove parent.cursorHover if it doesn't help optimize this at all.
		if(parent.cursorHover && element.shape(parent.bounds).intersects(mouseInverse)) {
			rt.addFirst(new GuiLevel(transform, true, bounds));
			mousePos = Optional.of(mouseInverse);
		} else {
			rt.addFirst(new GuiLevel(transform, false, bounds));
			mousePos = Optional.empty();
		}
		element.render(this, nanos);

		rt.removeFirst();
	}

	@Override
	public void play(Animation animation, Duration duration) {
		animations.add(new AnimationEvent(animation, System.nanoTime(), duration.toNanos()));
	}

	//TODO have this return whether a "UI user action" was consumed so it is known whether to apply it to other UI-like things such as the game world
	public void render(GuiElement gui, Framebuffer dest, Display display, ControlsQuery controls, long nanos, float guiScale) throws OpenGLException {
		if(!animations.isEmpty()) {
			long currentTime = System.nanoTime();
			while(!animations.isEmpty() && animations.peek().endTime() < currentTime) {
				animations.poll().animation.onEnd();
			}

			for(AnimationEvent event : animations) {
				long progressNanos = currentTime - event.start;
				//TODO nanos doesn't necessarily represent the update delta.
				event.animation().update(progressNanos, (float)((double)progressNanos/event.duration), nanos);
			}
		}

		dest.setDraw();
		int windowWidth = display.width();
		int windowHeight = display.height();

		//convert from pixel coords (from top left) to UI coords
		float scaleX = guiScale / windowWidth; float scaleY = guiScale / windowHeight;
		this.mouseX = Utility.convertRange(0, windowWidth, -1/ scaleX, 1/ scaleX, display.getCursorX());
		this.mouseY = -Utility.convertRange(0, windowHeight, -1/ scaleY, 1/ scaleY, display.getCursorY());
		this.nanos = nanos;
		this.controls = controls;
		program.loadViewMatrixScale(guiScale, windowWidth, windowHeight);
		program.setCurrent();
		//TODO cache the GuiLevels, possibly use dirty flag to determine if transforms need to change
		this.rt.addFirst(new GuiLevel(new RenderTransform(1, 0, 0, 1, 1, 0), true, new Rectangle(2/scaleX, 2/scaleY)));
		draw(gui, 1, 0, 0, 1, 1, 0);
		this.rt.removeFirst();
	}
}
