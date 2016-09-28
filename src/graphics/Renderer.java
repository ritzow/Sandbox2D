package graphics;

import input.handler.FramebufferSizeHandler;

import static util.MatrixMath.multiply;

import world.*;

public final class Renderer implements FramebufferSizeHandler {
	private ShaderProgram program;
	private volatile Camera camera;
	private int uniform_transform;
	private int uniform_opacity;
	private int uniform_view;
	
	private float framebufferWidth;
	private float framebufferHeight;
	
	public Renderer(ShaderProgram program, Camera camera, Display display) {	
		this.program = program;
		this.camera = camera;
		this.uniform_transform = program.getUniformID("transform");
		this.uniform_opacity = program.getUniformID("opacity");
		this.uniform_view = program.getUniformID("view");
		
		display.getInputManager().getFramebufferSizeHandlers().add(this);
	}
	
	public void loadTransformation(float[] transformation) {
		program.loadMatrix(uniform_transform, transformation);
	}
	
	public void loadView(float[] view) {
		program.loadMatrix(uniform_view, view);
	}
	
	public void loadOpacity(float opacity) {
		program.loadFloat(uniform_opacity, opacity);
	}
	
//	public void render(Model model, float[] transformation, float opacity) {
//		program.loadMatrix(uniform_transform, transformation);
//		program.loadFloat(uniform_opacity, opacity);
//		model.render();
//	}
	
//	public void render(World world) {
//		program.loadFloat(uniform_opacity, 1);
//		program.loadMatrix(uniform_view, getViewMatrix(camera));
//		
//		for(int row = 0; row < world.getBlocks().getHeight(); row++) {
//			for(int column = 0; column < world.getBlocks().getWidth(); column++) {
//				if(world.getBlocks().isBlock(column, row) && !world.getBlocks().isHidden(column, row)) {
//					Model blockModel = world.getBlocks().get(column, row).getModel();
//					program.loadMatrix(uniform_transform, getTransformationMatrix(column, row, 1, 1, 0));
//					blockModel.render();
//				}
//			}
//		}
//		
//		for(int i = 0; i < world.getEntities().size(); i++) {
//			Entity entity = world.getEntities().get(i);
//			render(entity.graphics().getModel(), getTransformationMatrix(entity), entity.graphics().getOpacity());
//		}
//	}
	
//	public void render(ContainerElement element, float x, float y) {
//		for(int i = 0; i < element.getElements().size(); i++) {
//			render(element.getElements().get(i), x, y);
//		}
//	}
	
//	public void render(Element element, float x, float y) {
//		if(element instanceof GraphicsElement) {
//			render((GraphicsElement)element, x, y);
//		}
//		
//		else if(element instanceof ContainerElement) {
//			render((ContainerElement)element, x, y);
//		}
//	}
	
//	public void render(ElementManager manager) {
//		for(Entry<Element, DynamicLocation> entry : manager.entrySet()) {
//			if(entry.getValue() != null)
//		    	render(entry.getKey(), ElementManager.getLocationX(entry.getValue(), framebufferHeight/framebufferWidth), ElementManager.getLocationY(entry.getValue(), 1));
//		}
//	}
	
//	public void render(GraphicsElement element, float x, float y) {
//		x += element.offset().getX();
//		y += element.offset().getY();
//		program.loadMatrix(uniform_view, getAspectMatrix());
//		float[] matrix = getTransformationMatrix(x,y,element.graphics().scale().getX(),element.graphics().scale().getY(),element.graphics().rotation().getRotation());
//		render(element.graphics().getModel(), matrix, element.graphics().getOpacity());
//	}
	
//	public float[] getTransformationMatrix(Entity entity) {
//		return getTransformationMatrix(entity.position(), entity.graphics());
//	}
	
//	public float[] getTransformationMatrix(Position position, Graphics graphics) {
//		return getTransformationMatrix(position.getX(), 
//				position.getY(), graphics.scale().getX(), 
//				graphics.scale().getY(), graphics.rotation().getRotation());
//	}
	
	public float[] getTransformationMatrix(float posX, float posY, float scaleX, float scaleY, float rotation) {		
		return new float[] {
				scaleX * (float) Math.cos(rotation), scaleY * (float) Math.sin(rotation), 0, posX,
				scaleX * (float) -Math.sin(rotation), scaleY * (float) Math.cos(rotation), 0, posY,
				0, 0, 0, 0,
				0, 0, 0, 1,
		};
	}
	
	public float[] getViewMatrix(Camera camera) {
		return multiply(getAspectMatrix(), getCameraMatrix(camera));
	}
	
	public float[] getCameraMatrix(Camera camera) {
		return new float[] {
				camera.getZoom(), 0, 0, -camera.getX() * camera.getZoom(),
				0, camera.getZoom(), 0, -camera.getY() * camera.getZoom(),
				0, 0, 0, 0,
				0, 0, 0, 1,
		};
	}
	
	public float[] getAspectMatrix() {
		return new float[] {
				framebufferHeight/framebufferWidth, 0, 0, 0,
				0, 1, 0, 0,
				0, 0, 0, 0,
				0, 0, 0, 1,
		};
	}

	public final ShaderProgram getProgram() {
		return program;
	}

	public final Camera getCamera() {
		return camera;
	}

	public final void setProgram(ShaderProgram program) {
		this.program = program;
	}

	public final void setCamera(Camera camera) {
		this.camera = camera;
	}

	@Override
	public void framebufferSize(int width, int height) {
		framebufferWidth = width;
		framebufferHeight = height;
	}
	
}