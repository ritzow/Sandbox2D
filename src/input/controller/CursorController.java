package input.controller;

import graphics.Camera;
import input.Controls;
import input.InputManager;
import input.handler.CursorPosHandler;
import input.handler.FramebufferSizeHandler;
import input.handler.KeyHandler;
import input.handler.MouseButtonHandler;
import main.Updatable;
import org.lwjgl.glfw.GLFW;
import util.ResourceManager;
import world.World;
import world.block.RedBlock;
import world.entity.Entity;
import world.entity.GenericEntity;

public final class CursorController implements MouseButtonHandler, CursorPosHandler, FramebufferSizeHandler, KeyHandler, Updatable {
	private boolean leftMouseDown;
	private boolean rightMouseDown;
	private boolean activatePressed;
	private volatile double mouseX;
	private volatile double mouseY;
	
	private World world;
	private Entity player;
	private Camera camera;
	
	private float frameWidth, frameHeight;
	
	public CursorController(InputManager manager, Entity player, World world, Camera camera) {
		this.world = world;
		this.player = player;
		this.camera = camera;
		manager.getMouseButtonHandlers().add(this);
		manager.getCursorPosHandlers().add(this);
		manager.getFramebufferSizeHandlers().add(this);
		manager.getKeyHandlers().add(this);
	}
	
	public void update() {
		update(camera, frameWidth, frameHeight);
	}
	
	private void update(Camera camera, float frameWidth, float frameHeight) {
		float worldX = (float)mouseX;
		float worldY = (float)mouseY;
		worldX = (2f * worldX) / frameWidth - 1f; //normalize the mouse coordinate
		worldY = -((2f * worldY) / frameHeight - 1f); //normalize the mouse coordinate
		worldX /= frameHeight/frameWidth; //apply aspect ratio
		worldX /= camera.getZoom();
		worldY /= camera.getZoom();
		worldX += camera.getX();
		worldY += camera.getY();
		
		int blockX = (int) Math.round(worldX);
		int blockY = (int) Math.round(worldY);
		
		float playerX = player.position().getX();
		float playerY = player.position().getY();
		
		double distance = Math.sqrt((playerX - blockX) * (playerX - blockX) + (playerY - blockY) * (playerY - blockY));
		
		if(distance <= 4) {
			if(leftMouseDown) {
				world.getBlocks().destroy(blockX, blockY);
			}
			
			else if(rightMouseDown) {
				if(world.getBlocks().isValid(blockX, blockY))
					world.getBlocks().place(blockX, blockY, new RedBlock());
			}
		}
		
		if(activatePressed) {
			activatePressed = false;
//			GenericEntity entity = new GenericEntity(ResourceManager.getModel("blue_square"));
//			entity.position().setX(worldX);
//			entity.position().setY(worldY);
//			entity.getHitbox().setWidth(5f);
//			entity.getHitbox().setHeight(5f);
//			entity.getGraphics().scale().setX(5f);
//			entity.getGraphics().scale().setY(5f);
//			world.getEntities().add(entity);
			
			for(int i = 0; i < 20; i++) {
				GenericEntity particle = new GenericEntity(ResourceManager.getModel("red_square"));
				particle.getGraphics().scale().setX(0.2f);
				particle.getGraphics().scale().setY(0.2f);
				particle.getHitbox().setWidth(0.2f);
				particle.getHitbox().setHeight(0.2f);
				particle.position().setX(worldX);
				particle.position().setY(worldY);
				particle.getHitbox().setPriority((float)Math.random());
				particle.getGraphics().rotation().setVelocity((float)Math.random() - 0.5f);
				particle.velocity().setX(-0.2f + ((float)Math.random() * (0.4f))); //min + ((float)Math.random() * (max - min))
				particle.velocity().setY((float)Math.random() * (0.35f));
				world.getEntities().add(particle);
			}
		}
	}
	
	@Override
	public void mouseButton(int button, int action, int mods) {
		if(button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
			leftMouseDown = true;
		}
		
		else if(button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_RELEASE) {
			leftMouseDown = false;
		}
		
		else if(button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
			rightMouseDown = true;
		}
		
		else if(button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_RELEASE) {
			rightMouseDown = false;
		}
	}

	@Override
	public void cursorPos(double xpos, double ypos) {
		mouseX = xpos;
		mouseY = ypos;
	}

	@Override
	public void framebufferSize(int width, int height) {
		frameWidth = width;
		frameHeight = height;
	}

	@Override
	public void keyboardButton(int key, int scancode, int action, int mods) {
		if(key == Controls.KEYBIND_ACTIVATE && action == GLFW.GLFW_PRESS) {
			activatePressed = true;
		}
	}
}