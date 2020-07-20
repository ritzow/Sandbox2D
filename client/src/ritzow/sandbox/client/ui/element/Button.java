package ritzow.sandbox.client.ui.element;

import java.time.Duration;
import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.ui.Animation;
import ritzow.sandbox.client.ui.GuiElement;
import ritzow.sandbox.client.ui.GuiRenderer;
import ritzow.sandbox.client.ui.Shape;
import ritzow.sandbox.util.Utility;

public class Button implements GuiElement {
	private final GuiElement content;
	private final Runnable action;

	private static final float HOVER_SCALE = 1.1f;

	public Button(GuiElement content, Runnable action) {
		this.content = content;
		this.action = action;
	}

	public Button(String text, Model model, Runnable action) {
		this.content = new HBox(0.2f, new Icon(model), new Text(text, RenderManager.FONT, 12, 0));
		this.action = action;
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		renderer.draw(content, 1, 0, 0, scale, scale, 0);
	}

	float scale = 1.0f;
	boolean anim = false;

	@Override
	public boolean render(GuiRenderer renderer, long nanos, float mouseX, float mouseY) {
		renderer.draw(content, 1, 0, 0, scale, scale, 0);
		if(!anim) {
			anim = true;
			renderer.play(new Animation() {
				@Override
				public void update(long progressNanos, float completionRatio, long nanosSinceLast) {
					//bouncing motion because it is half of sinusoid
					float temp = Utility.convertRange(0, 1, 1, HOVER_SCALE, (float)Math.sin(Math.PI * completionRatio));
					scale = temp;
				}

				@Override
				public void onEnd() {
					anim = false;
				}
			}, Duration.ofMillis(500));
		}
		//action.run();
		//TODO implement button state, pressing, releasing, press indicator (separate content?)
		return true;
	}

	@Override
	public Shape shape() { //TODO shape() needs to know about mouse hover event
		//return content.shape().scale(...)
		return content.shape().scale(scale);
	}
}
