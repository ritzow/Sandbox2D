package ritzow.sandbox.client.input.controller;

import org.lwjgl.glfw.GLFW;
import ritzow.sandbox.client.audio.Sounds;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.input.Controls;
import ritzow.sandbox.client.input.InputManager;
import ritzow.sandbox.client.input.handler.CursorPosHandler;
import ritzow.sandbox.client.input.handler.FramebufferSizeHandler;
import ritzow.sandbox.client.input.handler.KeyHandler;
import ritzow.sandbox.client.input.handler.MouseButtonHandler;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.entity.PlayerEntity;
import ritzow.sandbox.world.item.BlockItem;
import ritzow.sandbox.world.item.Item;

public final class InteractionController implements Controller, MouseButtonHandler, CursorPosHandler, FramebufferSizeHandler, KeyHandler {
	private boolean primaryAction, secondaryAction;
	private final boolean instantBreak;
	private volatile int frameWidth, frameHeight;
	private volatile int mouseX, mouseY;
	
	private long lastPlacement;
	private long lastBreak;
	private long cooldown;
	
	private World world;
	private PlayerEntity player;
	private Camera camera;
	
	public InteractionController(PlayerEntity player, World world, Camera camera, long cooldownMilliseconds, boolean instantBreak) {
		this.world = world;
		this.player = player;
		this.camera = camera;
		this.cooldown = cooldownMilliseconds;
		this.instantBreak = instantBreak;
	}
	
	@Override
	public void update() {
		update(camera, frameWidth, frameHeight);
	}
	
	protected void update(Camera camera, float frameWidth, float frameHeight) {
		float worldX = (2f * mouseX) / frameWidth - 1f; 		//normalize the mouse coordinate
		float worldY = -((2f * mouseY) / frameHeight - 1f);
		worldX /= frameHeight/frameWidth; 					//apply aspect ratio
		worldX /= camera.getZoom(); 								//apply zoom
		worldY /= camera.getZoom();
		worldX += camera.getPositionX(); 							//apply camera position
		worldY += camera.getPositionY();
		int blockX = Math.round(worldX);						//convert world coordinate to block grid coordinate
		int blockY = Math.round(worldY);
		float playerX = player.getPositionX();						//get player position
		float playerY = player.getPositionY();
		double distance = Math.sqrt((playerX - blockX) * (playerX - blockX) + (playerY - blockY) * (playerY - blockY));
		
		if(instantBreak || distance <= 4) {
			if(primaryAction && (instantBreak || System.nanoTime() - lastBreak > cooldown * 1000000)) {
				if(world.getForeground().isValid(blockX, blockY) && (world.getForeground().destroy(world, blockX, blockY) || world.getBackground().destroy(world, blockX, blockY))) {
					lastBreak = System.nanoTime();
				}
			}
			
			else if(secondaryAction && (System.nanoTime() - lastPlacement > cooldown * 1000000)) {
				Item item = player.getSelectedItem();
				if((item instanceof BlockItem) && world.getForeground().isValid(blockX, blockY) && 
					(world.getBackground().place(world, blockX, blockY, ((BlockItem)item).getBlock()) || 
					world.getForeground().place(world, blockX, blockY, ((BlockItem)item).getBlock()))) {
					lastPlacement = System.nanoTime();
					player.removeSelectedItem();
				}
			}
		}
	}
	
	private static float mouseHorizontalToWorld(Camera camera, float mouseX, int frameWidth, int frameHeight) {
		float worldX = (2f * mouseX) / frameWidth - 1f; 		//normalize the mouse coordinate
		worldX /= frameHeight/(float)frameWidth; 							//apply aspect ratio
		worldX /= camera.getZoom(); 								//apply zoom
		worldX += camera.getPositionX(); 							//apply camera position
		return worldX;
	}
	
	private static float mouseVerticalToWorld(Camera camera, float mouseY, int frameHeight) {
		float worldY = -((2f * mouseY) / frameHeight - 1f);
		worldY /= camera.getZoom();
		worldY += camera.getPositionY();
		return worldY;
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
		mouseX = (int) xpos;
		mouseY = (int) ypos;
	}

	@Override
	public void framebufferSize(int width, int height) {
		frameWidth = width;
		frameHeight = height;
	}

	@Override
	public void keyboardButton(int key, int scancode, int action, int mods) {
		if(action == GLFW.GLFW_PRESS) {
			switch(key) {
				case Controls.KEYBIND_DOWN:
					Item item = player.removeSelectedItem();
					if(item != null) {
						//TODO deal with entityID for entities created outside server directly
						ItemEntity entity = new ItemEntity(0, item);
						entity.setVelocityX(mouseHorizontalToWorld(camera, mouseX, frameWidth, frameHeight) - player.getPositionX());
						entity.setVelocityY(mouseVerticalToWorld(camera, mouseY, frameHeight) - player.getPositionY());
						entity.setSpeed(0.5f);
						entity.setPositionX(player.getPositionX() + 2 * player.getWidth() * entity.getVelocityX());
						entity.setPositionY(player.getPositionY() + 2 * player.getHeight() * entity.getVelocityY());
						world.getAudioSystem().playSound(Sounds.THROW, player.getPositionX(), player.getPositionY(), entity.getVelocityX(), entity.getVelocityY(), 1.0f, 1f);
						world.add(entity);
					}
					break;
				case GLFW.GLFW_KEY_KP_1:
				case GLFW.GLFW_KEY_1:
					player.setSlot(0);
					break;
				case GLFW.GLFW_KEY_KP_2:
				case GLFW.GLFW_KEY_2:
					player.setSlot(1);
					break;
				case GLFW.GLFW_KEY_KP_3:
				case GLFW.GLFW_KEY_3:
					player.setSlot(2);
					break;
				case GLFW.GLFW_KEY_KP_4:
				case GLFW.GLFW_KEY_4:
					player.setSlot(3);
					break;
				case GLFW.GLFW_KEY_KP_5:
				case GLFW.GLFW_KEY_5:
					player.setSlot(4);
					break;
			    case GLFW.GLFW_KEY_KP_6:
				case GLFW.GLFW_KEY_6:
					player.setSlot(5);
					break;
				case GLFW.GLFW_KEY_KP_7:
				case GLFW.GLFW_KEY_7:
					player.setSlot(6);
					break;
				case GLFW.GLFW_KEY_KP_8:
				case GLFW.GLFW_KEY_8:
					player.setSlot(7);
					break;
			    case GLFW.GLFW_KEY_KP_9:
				case GLFW.GLFW_KEY_9:
					player.setSlot(8);
					break;
			}	
		}
	}
}