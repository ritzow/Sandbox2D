package input.controller;

import graphics.Camera;
import input.Controls;
import input.InputManager;
import input.handler.CursorPosHandler;
import input.handler.FramebufferSizeHandler;
import input.handler.KeyHandler;
import input.handler.MouseButtonHandler;
import org.lwjgl.glfw.GLFW;
import resource.Models;
import util.Updatable;
import world.World;
import world.block.Block;
import world.block.RedBlock;
import world.entity.GraphicsEntity;
import world.entity.Player;

public final class InteractionController extends Controller implements MouseButtonHandler, CursorPosHandler, FramebufferSizeHandler, KeyHandler, Updatable {
	private boolean leftMouseDown;
	private boolean rightMouseDown;
	private boolean activatePressed;
	
	private float frameWidth, frameHeight;
	private volatile double mouseX, mouseY;
	
	private long lastPlacement;
	private long lastBreak;
	private float cooldown;
	private Block block;
	
	private World world;
	private Player player;
	private Camera camera;
	
	public InteractionController(Player player, World world, Camera camera, float cooldownMilliseconds) {
		this.block = new RedBlock();
		this.world = world;
		this.player = player;
		this.camera = camera;
		this.cooldown = cooldownMilliseconds;
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
		worldX += camera.getPositionX();
		worldY += camera.getPositionY();
		
		int blockX = (int) Math.round(worldX);
		int blockY = (int) Math.round(worldY);
		
		float playerX = player.getPositionX();
		float playerY = player.getPositionY();
		
		double distance = Math.sqrt((playerX - blockX) * (playerX - blockX) + (playerY - blockY) * (playerY - blockY));
		
		if(distance <= 4) {
			if(leftMouseDown && (System.currentTimeMillis() - lastBreak > cooldown)) {
				if(world.getForeground().isValid(blockX, blockY) && (world.getForeground().destroy(blockX, blockY) || world.getBackground().destroy(blockX, blockY))) {
					lastBreak = System.currentTimeMillis();
				}
			}
			
			else if(rightMouseDown && (System.currentTimeMillis() - lastPlacement > cooldown)) {
				if(world.getForeground().isValid(blockX, blockY) && (world.getBackground().place(blockX, blockY, block.createNew()) || world.getForeground().place(blockX, blockY, block.createNew()))) {
					lastPlacement = System.currentTimeMillis();
				}
			}
		}
		
		if(activatePressed) {
			GraphicsEntity entity = new GraphicsEntity(Models.BLUE_SQUARE);
			entity.setPositionX(worldX);
			entity.setPositionY(worldY);
			entity.setWidth(3);
			entity.setHeight(3);
			entity.setMass(10);
			entity.getGraphics().setScaleX(3);
			entity.getGraphics().setScaleY(3);
			world.getEntities().add(entity);
			activatePressed = false;
		}
	}
	

	@Override
	public void link(InputManager input) {
		input.getCursorPosHandlers().add(this);
		input.getFramebufferSizeHandlers().add(this);
		input.getKeyHandlers().add(this);
		input.getMouseButtonHandlers().add(this);
	}
	
	@Override
	public void unlink(InputManager input) {
		input.getCursorPosHandlers().remove(this);
		input.getFramebufferSizeHandlers().remove(this);
		input.getKeyHandlers().remove(this);
		input.getMouseButtonHandlers().remove(this);
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
		
		else if(key == GLFW.GLFW_KEY_L && action == GLFW.GLFW_PRESS) {
			player.dropItem(world, player.getSelectedSlot());
		}
		
		else if(key == GLFW.GLFW_KEY_C && action == GLFW.GLFW_PRESS) {
			
		}
		
		else if(action == GLFW.GLFW_PRESS) {	
			if(key == GLFW.GLFW_KEY_KP_1 || key == GLFW.GLFW_KEY_1) {
				player.setSelected(0);
			}
			
			else if(key == GLFW.GLFW_KEY_KP_2 || key == GLFW.GLFW_KEY_2) {
				player.setSelected(1);
			}
			
			else if(key == GLFW.GLFW_KEY_KP_3 || key == GLFW.GLFW_KEY_3) {
				player.setSelected(2);
			}
			
			else if(key == GLFW.GLFW_KEY_KP_4 || key == GLFW.GLFW_KEY_4) {
				player.setSelected(3);
			}
			
			else if(key == GLFW.GLFW_KEY_KP_5 || key == GLFW.GLFW_KEY_5) {
				player.setSelected(4);
			}
			
			else if(key == GLFW.GLFW_KEY_KP_6 || key == GLFW.GLFW_KEY_6) {
				player.setSelected(5);
			}
			
			else if(key == GLFW.GLFW_KEY_KP_7 || key == GLFW.GLFW_KEY_7) {
				player.setSelected(6);
			}
			
			else if(key == GLFW.GLFW_KEY_KP_8 || key == GLFW.GLFW_KEY_8) {
				player.setSelected(7);
			}
			
			else if(key == GLFW.GLFW_KEY_KP_9 || key == GLFW.GLFW_KEY_9) {
				player.setSelected(8);
			}
		}
	}
}