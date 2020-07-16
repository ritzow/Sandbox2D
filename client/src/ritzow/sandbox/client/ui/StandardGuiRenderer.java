package ritzow.sandbox.client.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.ui.element.GuiElement;

public class StandardGuiRenderer implements GuiRenderer {
	private final ModelRenderer program;
	private final Deque<Position> offsets;

	public StandardGuiRenderer(ModelRenderer modelProgram) {
		this.program = modelProgram;
		this.offsets = new ArrayDeque<>();
		this.offsets.add(Position.of(0, 0));
	}

	@Override
	public void draw(Graphics graphics, float offsetX, float offsetY) {
		Position relativeTo = offsets.peekFirst();
		program.queueRender(graphics.getModel(), graphics.getOpacity(),
			relativeTo.x() + offsetX, relativeTo.y() + offsetY, graphics.getScaleX(),
			graphics.getScaleY(), graphics.getRotation());
	}

	@Override
	public void draw(Model model, float opacity, float posX, float posY, float scaleX, float scaleY, float rotation) {
		Position relativeTo = offsets.peekFirst();
		program.queueRender(model, opacity,
			relativeTo.x() + posX, relativeTo.y() + posY, scaleX,
			scaleY, rotation);
	}

	@Override
	public void draw(GuiElement element, float posX, float posY) {
		Position relativeTo = offsets.peekFirst();
		offsets.addFirst(Position.of(relativeTo.x() + posX, relativeTo.y() + posY));
		element.render(this);
		offsets.removeFirst();
	}

	public void render(GuiElement gui, Framebuffer dest, int framebufferWidth, int framebufferHeight) throws OpenGLException {
		dest.clear(161 / 256f, 252 / 256f, 156 / 256f, 1.0f);
		dest.setDraw();

		//load the view transformation
		program.loadViewMatrixStandard(framebufferWidth, framebufferHeight);

		//set the current shader program
		program.setCurrent();

		draw(gui, 0, 0);
	}
}
