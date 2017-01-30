package ritzow.solomon.engine.world.entity;

import ritzow.solomon.engine.util.ByteUtil;

public abstract class LivingEntity extends Entity {
	protected int maxHealth;
	protected int health;
	
	public LivingEntity(int maxHealth) {
		this.maxHealth = maxHealth;
		this.health = maxHealth;
	}
	
	public LivingEntity(byte[] data) {
		super(data);
		maxHealth = ByteUtil.getInteger(data, 20);
		health = ByteUtil.getInteger(data, 24);
	}
	
	public byte[] getBytes() {
		byte[] sb = super.getBytes();
		byte[] data = new byte[sb.length + 8];
		System.arraycopy(sb, 0, data, 0, sb.length);
		ByteUtil.putInteger(data, sb.length, maxHealth);
		ByteUtil.putInteger(data, sb.length + 4, health);
		return data;
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
