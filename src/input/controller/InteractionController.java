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
import world.entity.Player;
import world.entity.TestEntity;
import world.item.BlockItem;

public final class InteractionController extends Controller implements MouseButtonHandler, CursorPosHandler, FramebufferSizeHandler, KeyHandler, Updatable {
	private boolean primaryAction, secondaryAction, tertiaryAction;
	private float frameWidth, frameHeight;
	private volatile double mouseX, mouseY;
	
	private long lastPlacement;
	private long lastBreak;
	private long cooldown;
	
	private World world;
	private Player player;
	private Camera camera;
	
	public InteractionController(Player player, World world, Camera camera, long cooldownMilliseconds) {
		this.world = world;
		this.player = player;
		this.camera = camera;
		this.cooldown = cooldownMilliseconds;
	}
	
	public void update() {
		update(camera, frameWidth, frameHeight);
	}
	
	protected void update(Camera camera, float frameWidth, float frameHeight) {
		float worldX = (2f * (float)mouseX) / frameWidth - 1f; 		//normalize the mouse coordinate
		float worldY = -((2f * (float)mouseY) / frameHeight - 1f);
		worldX /= frameHeight/frameWidth; 							//apply aspect ratio
		worldX /= camera.getZoom(); 								//apply zoom
		worldY /= camera.getZoom();
		worldX += camera.getPositionX(); 							//apply camera position
		worldY += camera.getPositionY();
		int blockX = (int)Math.round(worldX);						//convert world coordinate to block grid coordinate
		int blockY = (int)Math.round(worldY);
		float playerX = player.getPositionX();						//get player position
		float playerY = player.getPositionY();
		double distance = Math.sqrt((playerX - blockX) * (playerX - blockX) + (playerY - blockY) * (playerY - blockY));
		
		if(distance <= 4) {
			if(primaryAction && (System.currentTimeMillis() - lastBreak > cooldown)) {
				if(world.getForeground().isValid(blockX, blockY) && (world.getForeground().destroy(blockX, blockY) || world.getBackground().destroy(blockX, blockY))) {
					lastBreak = System.currentTimeMillis();
				}
			}
			
			else if(secondaryAction && (System.currentTimeMillis() - lastPlacement > cooldown)) {
				if((player.getSelectedItem() instanceof BlockItem) && 
					world.getForeground().isValid(blockX, blockY) && 
					(world.getBackground().place(blockX, blockY, ((BlockItem)player.getSelectedItem()).getBlock()) || 
					world.getForeground().place(blockX, blockY, ((BlockItem)player.getSelectedItem()).getBlock()))) {
					lastPlacement = System.currentTimeMillis();
					player.removeSelectedItem();
				}
			}
		}
		
		if(tertiaryAction) {
			TestEntity entity = new TestEntity(Models.BLUE_SQUARE, 3, 3, worldX, worldY);
			entity.getGraphics().setScaleX(3);
			entity.getGraphics().setScaleY(3);
			world.add(entity);
			tertiaryAction = false;
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
			primaryAction = true;
		}
		
		else if(button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_RELEASE) {
			primaryAction = false;
		}
		
		else if(button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
			secondaryAction = true;
		}
		
		else if(button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_RELEASE) {
			secondaryAction = false;
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
			tertiaryAction = true;
		}
		
		else if(key == GLFW.GLFW_KEY_L && action == GLFW.GLFW_PRESS) {
			player.dropSelectedItem(world);
		}
		
		else if(key == GLFW.GLFW_KEY_C && action == GLFW.GLFW_PRESS) {
			
		}
		
		else if(action == GLFW.GLFW_PRESS) {	
			if(key == GLFW.GLFW_KEY_KP_1 || key == GLFW.GLFW_KEY_1) {
				player.setSlot(0);
			}
			
			else if(key == GLFW.GLFW_KEY_KP_2 || key == GLFW.GLFW_KEY_2) {
				player.setSlot(1);
			}
			
			else if(key == GLFW.GLFW_KEY_KP_3 || key == GLFW.GLFW_KEY_3) {
				player.setSlot(2);
			}
			
			else if(key == GLFW.GLFW_KEY_KP_4 || key == GLFW.GLFW_KEY_4) {
				player.setSlot(3);
			}
			
			else if(key == GLFW.GLFW_KEY_KP_5 || key == GLFW.GLFW_KEY_5) {
				player.setSlot(4);
			}
			
			else if(key == GLFW.GLFW_KEY_KP_6 || key == GLFW.GLFW_KEY_6) {
				player.setSlot(5);
			}
			
			else if(key == GLFW.GLFW_KEY_KP_7 || key == GLFW.GLFW_KEY_7) {
				player.setSlot(6);
			}
			
			else if(key == GLFW.GLFW_KEY_KP_8 || key == GLFW.GLFW_KEY_8) {
				player.setSlot(7);
			}
			
			else if(key == GLFW.GLFW_KEY_KP_9 || key == GLFW.GLFW_KEY_9) {
				player.setSlot(8);
			}
		}
	}
}