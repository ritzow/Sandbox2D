package world.item;

import util.Transportable;
import world.entity.component.Graphics;

public abstract class Item implements Transportable {
	public abstract Graphics getGraphics();
	public abstract String getName();
}
