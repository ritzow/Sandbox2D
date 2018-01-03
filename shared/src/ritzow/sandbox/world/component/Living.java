package ritzow.sandbox.world.component;

public interface Living {
	public int getHealth();
	public int getMaxHealth();
	public void setHealth(int health);
	//TODO add things like getters for different sounds (damage, healing, etc.)
}
