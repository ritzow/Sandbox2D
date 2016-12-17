package world.entity;

import util.ByteUtil;

public abstract class LivingEntity extends Entity {
	protected int maxHealth;
	protected int health;
	
	public LivingEntity(int maxHealth) {
		this.maxHealth = maxHealth;
		this.health = maxHealth;
	}
	
	public LivingEntity(byte[] data) {
		super(data);
		maxHealth = ByteUtil.getInteger(data, 16);
		health = ByteUtil.getInteger(data, 20);
	}
	
	public byte[] toBytes() {
		byte[] sb = super.toBytes();
		byte[] data = new byte[sb.length + 8];
		System.arraycopy(sb, 0, data, 0, sb.length);
		ByteUtil.putInteger(data, 16, maxHealth);
		ByteUtil.putInteger(data, 20, health);
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
