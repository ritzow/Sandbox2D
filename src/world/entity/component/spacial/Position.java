package world.entity.component.spacial;

import world.entity.component.Vector;

public final class Position extends Vector {

	public Position(float x, float y) {
		super(x, y);
	}
	
	public void update(Velocity velocity, float milliseconds) {
		this.x += velocity.getX() * milliseconds;
		this.y += velocity.getY() * milliseconds;
	}
	
}
