package world.entity.component.visual;

import graphics.Model;

public class Graphics {
	protected Model model;
	protected Scale scale;
	protected Rotation rotation;
	
	protected float opacity;

	public Graphics(Model model) {
		this.model = model;
		this.scale = new Scale(1, 1);
		this.rotation = new Rotation(0);
		this.opacity = 1;
	}
	
	public Scale scale() {
		return scale;
	}
	
	public Rotation rotation() {
		return rotation;
	}
	
	public Model getModel() {
		return model;
	}
	
	public void setModel(Model model) {
		this.model = model;
	}
	
	public float getOpacity() {
		return opacity;
	}
	
	public void setOpacity(float opacity) {
		this.opacity = opacity;
	}
	
}
