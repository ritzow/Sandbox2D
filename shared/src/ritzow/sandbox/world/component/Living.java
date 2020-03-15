package ritzow.sandbox.world.component;

public interface Living {
	int getHealth();
	int getMaxHealth();
	void setHealth(int health);
	//TODO add things like getters for different sounds (damage, healing, etc.)
}
