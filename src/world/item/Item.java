package world.item;

import java.io.Externalizable;
import world.entity.component.Graphics;

public abstract class Item implements Externalizable {
	public abstract Graphics getGraphics();
	public abstract String getName();
}
