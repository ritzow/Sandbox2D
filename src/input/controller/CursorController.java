package input.controller;

import graphics.Camera;
import input.Controls;
import input.InputManager;
import input.handler.CursorPosHandler;
import input.handler.FramebufferSizeHandler;
import input.handler.KeyHandler;
import input.handler.MouseButtonHandler;
import org.lwjgl.glfw.GLFW;
import util.ModelManager;
import util.Updatable;
import world.World;
import world.block.Block;
import world.block.RedBlock;
import world.entity.Entity;
import world.entity.GenericEntity;

public final class CursorController implements MouseButtonHandler, CursorPosHandler, FramebufferSizeHandler, KeyHandler, Updatable {
	private boolean leftMouseDown;
	private boolean rightMouseDown;
	private boolean activatePressed;
	private volatile double mouseX;
	private volatile double mouseY;
	private long lastPlacement;
	private long lastBreak;
	
	private Block block;
	
	private World world;
	private Entity player;
	private Camera camera;
	
	private float frameWidth, frameHeight;
	
	private float cooldown;
	
	public CursorController(InputManager manager, Entity player, World world, Camera camera, float cooldownMilliseconds) {
		this.block = new RedBlock();
		this.world = world;
		this.player = player;
		this.camera = camera;
		this.cooldown = cooldownMilliseconds;
		manager.getMouseButtonHandlers().add(this);
		manager.getCursorPosHandlers().add(this);
		manager.getFramebufferSizeHandlers().add(this);
		manager.getKeyHandlers().add(this);
	}
	
	public void setBlock(Block block) {
		this.block = block;
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
		
		float playerX = player.getPositionX();
		float playerY = player.getPositionY();
		
		double distance = Math.sqrt((playerX - blockX) * (playerX - blockX) + (playerY - blockY) * (playerY - blockY));
		
		if(distance <= 4) {
			if(leftMouseDown && (System.currentTimeMillis() - lastBreak > cooldown)) {
				if(world.getBlocks().isValid(blockX, blockY) && world.getBlocks().destroy(blockX, blockY)) {
					lastBreak = System.currentTimeMillis();
				}
			}
			
			else if(rightMouseDown && (System.currentTimeMillis() - lastPlacement > cooldown)) {
				if(world.getBlocks().isValid(blockX, blockY) && world.getBlocks().place(blockX, blockY, block.createNew())) {
					lastPlacement = System.currentTimeMillis();
				}
			}
		}
		
		if(activatePressed) {
			activatePressed = false;
			GenericEntity entity = new GenericEntity(ModelManager.BLUE_SQUARE);
			entity.setPositionX(worldX);
			entity.setPositionY(worldY);
			entity.getHitbox().setWidth(3f);
			entity.getHitbox().setHeight(3f);
			entity.getGraphics().scale().setX(3f);
			entity.getGraphics().scale().setY(3f);
			entity.setVelocityX((float)Math.random() * 2 - 1);
			entity.setVelocityY((float)Math.random() * 2 - 1);	
			world.getEntities().add(entity);
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