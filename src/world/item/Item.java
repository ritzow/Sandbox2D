package world.item;

import graphics.Model;

public abstract class Item {
	protected Model model;
	protected float mass;
	
	public final Model getModel() {
		return model;
	}
	
	public final float getMass() {
		return mass;
	}
	
	public final void setModel(Model model) {
		this.model = model;
	}
	
	public final void setMass(float mass) {
		this.mass = mass;
	}
	
}
