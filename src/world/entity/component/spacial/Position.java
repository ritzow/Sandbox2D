package world.entity.component.spacial;

import java.io.Serializable;
import world.entity.component.Vector;

public final class Position extends Vector implements Serializable {
	private static final long serialVersionUID = -1858323437642061827L;

	public Position(float x, float y) {
		super(x, y);
	}
	
	public void update(Velocity velocity, float milliseconds) {
		this.x += velocity.getX() * milliseconds;
		this.y += velocity.getY() * milliseconds;
	}
	
}
