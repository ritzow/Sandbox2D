package world.entity.component;

import graphics.Model;
import java.io.Serializable;

public class Graphics implements Serializable {
	private static final long serialVersionUID = 4497437671840075738L;
	
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
	
	public Scale getScale() {
		return scale;
	}
	
	public Rotation getRotation() {
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
