package world.entity;

import graphics.Renderer;
import java.util.ArrayList;

public class ContainerEntity extends Entity {
	
	protected final ArrayList<Entity> entities;
	
	public ContainerEntity() { //TODO make position of sub-entities relative to container entity
		entities = new ArrayList<Entity>();
	}
	
//	public void update(float milliseconds) {
//		
//		super.update(milliseconds);
//		
//		for(int i = 0; i < entities.size(); i++) {
//			Entity e = entities.get(i);
//			e.position().setX(this.position().getX() + e.position().getX());
//			e.position().setY(this.position().getY() + e.position().getY());
//			e.update(milliseconds);
//		}
//	}
	
	public ArrayList<Entity> getEntities() {
		return entities;
	}

	@Override
	public void render(Renderer renderer) {
		for(int i = 0; i < entities.size(); i++) {
			entities.get(i).render(renderer);
		}
	}
}
