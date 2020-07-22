package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.client.graphics.RenderManager;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.ui.GuiElement;
import ritzow.sandbox.client.ui.GuiRenderer;
import ritzow.sandbox.client.ui.Shape;

public class Button implements GuiElement {
	private final GuiElement content;
	private final Runnable action;
	private float scale = 1.0f;

	private static final float HOVER_SCALE = 1.1f, PRESS_SCALE = 0.9f, STANDARD_SCALE = 1.0f;

	public Button(GuiElement content, Runnable action) {
		this.content = content;
		this.action = action;
	}

	public Button(String text, Model model, Runnable action) {
		this.content = new HBox(0.1f, new Scaler(new Icon(model), 0.4f), new Text(text, RenderManager.FONT, 8, 0));
		this.action = action;
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		//always use the original scale for the mouse click area size
		if(renderer.mousePos().map(content.shape()::intersects).orElse(false)) {
			float scale = switch(renderer.controls().state(Control.UI_ACTIVATE)) {
				case PREVIOUSLY_PRESSED, NEWLY_PRESSED -> PRESS_SCALE;

				case NEWLY_RELEASED -> {
					action.run();
					yield 1;
				}

				default -> HOVER_SCALE;
			};

			renderer.draw(content, 1.0f, 0.0f, 0.0f, scale, scale, 0.0f);
			this.scale = Math.max(1, scale);
		} else {
			renderer.draw(content);
			scale = 1;
		}

//		if(!anim) {
//			anim = true;
//			renderer.play(new Animation() {
//				@Override
//				public void update(long progressNanos, float completionRatio, long nanosSinceLast) {
//					//bouncing motion because it is half of sinusoid
//					float temp = Utility.convertRange(0, 1, 1, HOVER_SCALE, (float)Math.sin(Math.PI * completionRatio));
//					scale = temp;
//				}
//
//				@Override
//				public void onEnd() {
//					anim = false;
//				}
//			}, Duration.ofMillis(500));
//		}
	}

	@Override
	public Shape shape() {
		return content.shape().scale(scale);
	}
}
