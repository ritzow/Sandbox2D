package ritzow.sandbox.client.ui.element;

import java.util.ArrayList;
import java.util.List;
import ritzow.sandbox.client.graphics.Model;
import ritzow.sandbox.client.ui.*;

public class EditableText implements GuiElement {
	private final List<Entry> content;
	private final Font font;
	private final float scale, spacing;

	private static final float SIZE_SCALE = 0.002f;

	private static record Entry(Model model, int character) {}

	public EditableText(Font font, float size, float spacing) {
		this.font = font;
		this.scale = size * SIZE_SCALE;
		this.spacing = spacing;
		this.content = new ArrayList<>();
	}

	@Override
	public void render(GuiRenderer renderer, long nanos) {
		float pos = -width()/2f;
		for(Entry e : content) {
			pos += charWidth()/2f;
			renderer.draw(e.model, 1, pos, 0, scale, scale, 0);
			pos += charWidth()/2f + spacing;
		}
	}

	private float charWidth() {
		return scale * font.getGlyphWidth();
	}

	public boolean hasContent() {
		return !content.isEmpty();
	}

	public int length() {
		return content.size();
	}

	public void append(int character) {
		content.add(entry(character));
	}

	public void append(String text) {
		text.codePoints().mapToObj(this::entry).forEachOrdered(content::add);
	}

	public void delete() {
		content.remove(content.size() - 1);
	}

	public void delete(int index) {
		content.remove(index);
	}

	public void insert(int index, int character) {
		content.add(index, entry(character));
	}

	public void delete(int index, int count) {
		content.subList(index, index + count).clear();
	}

	public void insert(int index, String text) {
		text.codePoints().mapToObj(this::entry).forEachOrdered(entry -> content.add(index, entry));
	}

	//TODO only recompute when a character is added or removed
	private float width() {
		return font.getGlyphWidth() * content.size() * scale + Math.max(content.size() - 1, 0) * spacing;
	}

	private float height() {
		return font.getGlyphHeight() * scale;
	}

	public EditableText setContent(CharSequence text) {
		content.clear();
		text.codePoints().mapToObj(this::entry).forEachOrdered(content::add);
		return this;
	}

	private Entry entry(int character) {
		return new Entry(getModel(character), character);
	}

	private Model getModel(int character) {
		Model model = font.getModel(character);
		if(model == null) {
			throw new IllegalArgumentException("Content string contains unknown character '"
				+ Character.toString(character));
		}
		return model;
	}

	public String content() {
		int[] codePoints = content.stream().mapToInt(Entry::character).toArray();
		return new String(codePoints, 0, codePoints.length);
	}

	@Override
	public Shape shape() {
		return new Rectangle(width(), height());
	}
}
