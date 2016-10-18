package world.entity;

public abstract class LivingEntity extends Entity {
	
	private static final long serialVersionUID = 1397868054064772835L;
	
	protected int maxHealth;
	protected int health;
	
	public LivingEntity(int maxHealth) {
		this.maxHealth = maxHealth;
		this.health = maxHealth;
	}
	
	public final int getMaxHealth() {
		return maxHealth;
	}
	
	public final int getHealth() {
		return health;
	}
	
	public final void setMaxHealth(int maxHealth) {
		this.maxHealth = maxHealth;
	}
	
	public final void setHealth(int health) {
		this.health = Math.min(health, maxHealth);
	}
	
	
}
