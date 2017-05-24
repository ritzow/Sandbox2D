package ritzow.sandbox.world.component;

import ritzow.sandbox.audio.Sound;

public interface Living {
	public int getHealth();
	public int getMaxHealth();
	public void setHealth(int health);
	default public Sound getDamageSound() {
		throw new UnsupportedOperationException("not implemented"); //TODO create default damage sound
	}
}
