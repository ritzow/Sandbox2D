package ritzow.sandbox.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.ui.element.UIElement;

public class UserInterface implements Renderer {
	private final ModelRenderer modelProgram;
	private final List<Entry<UIElement, Position>> elements;

	public static final class Position {
		float x;
		float y;

		private Position(float x, float y) {
			this.x = x;
			this.y = y;
		}

		public static Position of(float x, float y) {
			return new Position(x, y);
		}
	}

	@SafeVarargs
	public static UserInterface of(ModelRenderer program, Entry<UIElement, Position>... elements) {
		return new UserInterface(program, elements);
	}

	private UserInterface(ModelRenderer modelProgram, Entry<UIElement, Position>[] elements) {
		this.modelProgram = modelProgram;
		this.elements = new ArrayList<>(List.of(elements));
	}

	@Override
	public void render(Framebuffer dest, int framebufferWidth, int framebufferHeight) throws OpenGLException {
		dest.clear(161/256f, 252/256f, 156/256f, 1.0f);
		dest.setDraw();

		//ensure that model program is cached on stack
		ModelRenderer program = this.modelProgram;

		//load the view transformation
		program.loadViewMatrixStandard(framebufferWidth, framebufferHeight);

		//set the current shader program
		program.setCurrent();

		for(var element : elements) {
			for(Graphics g : element.getKey().appearance()) {
				Position p = element.getValue();
				program.queueRender(g.getModel(), g.getOpacity(), p.x, p.y,
						g.getScaleX(), g.getScaleY(), g.getRotation());
			}
		}
	}

	public void update(Display display, long nanoseconds) {
		for(var element : elements) {
			element.getKey().update(nanoseconds);
		}
		//int cursorX = display.getCursorX(), cursorY = display.getCursorY();
	}
}
