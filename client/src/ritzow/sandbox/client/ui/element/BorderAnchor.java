package ritzow.sandbox.client.ui.element;

import ritzow.sandbox.client.ui.*;

public class BorderAnchor implements GuiElement {

	//TODO add corners as well, maybe remove the other offset value
	public enum Side {
		LEFT {
			@Override
			Position translate(float x, float y, Rectangle bounds, Rectangle border) {
				return Position.of((bounds.width() - border.width())/2f + x, y);
			}
		},
		RIGHT {
			@Override
			Position translate(float x, float y, Rectangle bounds, Rectangle border) {
				return Position.of((border.width() - bounds.width())/2f - x, y);
			}
		},
		BOTTOM {
			@Override
			Position translate(float x, float y, Rectangle bounds, Rectangle border) {
				return Position.of(x, (bounds.height() - border.height())/2f + y);
			}
		},
		TOP {
			@Override
			Position translate(float x, float y, Rectangle bounds, Rectangle border) {
				return Position.of(x, (border.height() - bounds.height())/2f - y);
			}
		};

		abstract Position translate(float x, float y, Rectangle bounds, Rectangle border);
	}

	public static record Anchor(GuiElement element, Side side, float x, float y) {}

	private final Anchor[] anchors;

	public BorderAnchor(Anchor... elements) {
		//TODO might be able to store positions as normalized in -1 to 1 space, then multiply by renderer.parent() bounds to get proper positions
		//however, this might not anchor them to the sides and instead scale them with them, not sure.
		this.anchors = elements;
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		Rectangle parent = renderer.parent();
		for(Anchor anchor : anchors) {
			Rectangle bounds = anchor.element.shape(parent).toRectangle();
			Position translated = anchor.side.translate(anchor.x, anchor.y, bounds, parent);
			renderer.draw(anchor.element, translated.x(), translated.y());
		}
	}

	@Override
	public Shape shape() {
		return new Rectangle(1, 1); //TODO implement minimum size so elements don't overlap or which is pleasing to the eye
	}

	@Override
	public Shape shape(Rectangle parent) {
		return parent;
	}
}
