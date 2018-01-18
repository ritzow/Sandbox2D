package ritzow.sandbox.client.input.controller;

import org.lwjgl.glfw.GLFW;
import ritzow.sandbox.client.Client;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.input.InputManager;
import ritzow.sandbox.client.input.handler.CursorPosHandler;
import ritzow.sandbox.client.input.handler.FramebufferSizeHandler;
import ritzow.sandbox.client.input.handler.KeyHandler;
import ritzow.sandbox.client.input.handler.MouseButtonHandler;
import ritzow.sandbox.client.world.item.ClientBlockItem;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.item.Item;

public final class InteractionController implements Controller, MouseButtonHandler, CursorPosHandler, FramebufferSizeHandler, KeyHandler {
	private volatile boolean primaryAction, secondaryAction;
	private volatile int frameWidth, frameHeight;
	private volatile int mouseX, mouseY;
	
	private long cooldownPlace, cooldownBreak;
	private long lastPlace, lastBreak;
	private float range;
	
	private final Camera camera;
	private final Client client;
	
	public InteractionController(Client client, Camera camera, long breakCooldown, long placeCooldown, float range) {
		this.client = client;
		this.camera = camera;
		this.cooldownBreak = breakCooldown;
		this.cooldownPlace = placeCooldown;
		this.range = range;
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
		float playerX = client.getPlayer().getPositionX();						//get player position
		float playerY = client.getPlayer().getPositionY();
		
		if(true || Utility.withinDistance(playerX, playerY, blockX, blockY, range)) {
			if(primaryAction && !breakCooldownActive()) {
				if(client.getWorld().getForeground().isBlock(blockX, blockY)) {
					client.sendBlockBreak(blockX, blockY);
					lastBreak = System.nanoTime();
				}
			} else if(secondaryAction && !placeCooldownActive()) {
				Item item = client.getPlayer().getSelectedItem();
				if((item instanceof ClientBlockItem) && client.getWorld().getForeground().isValid(blockX, blockY) && 
					(client.getWorld().getBackground().place(client.getWorld(), blockX, blockY, ((ClientBlockItem)item).getBlock()) || 
					client.getWorld().getForeground().place(client.getWorld(), blockX, blockY, ((ClientBlockItem)item).getBlock()))) {
					client.getPlayer().removeSelectedItem();
					lastPlace = System.nanoTime();
				}
			}
		}
	}
	
	private boolean breakCooldownActive() {
		return false && System.nanoTime() - lastBreak < cooldownBreak * 1000000;
	}
	
	private boolean placeCooldownActive() {
		return System.nanoTime() - lastPlace < cooldownPlace * 1000000;
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
		primaryAction = ((button == GLFW.GLFW_MOUSE_BUTTON_LEFT || primaryAction) && action == GLFW.GLFW_PRESS);
		secondaryAction = ((button == GLFW.GLFW_MOUSE_BUTTON_RIGHT || secondaryAction) && action == GLFW.GLFW_PRESS);
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
				case GLFW.GLFW_KEY_KP_1:
				case GLFW.GLFW_KEY_1:
					client.getPlayer().setSlot(0);
					break;
				case GLFW.GLFW_KEY_KP_2:
				case GLFW.GLFW_KEY_2:
					client.getPlayer().setSlot(1);
					break;
				case GLFW.GLFW_KEY_KP_3:
				case GLFW.GLFW_KEY_3:
					client.getPlayer().setSlot(2);
					break;
				case GLFW.GLFW_KEY_KP_4:
				case GLFW.GLFW_KEY_4:
					client.getPlayer().setSlot(3);
					break;
				case GLFW.GLFW_KEY_KP_5:
				case GLFW.GLFW_KEY_5:
					client.getPlayer().setSlot(4);
					break;
			    case GLFW.GLFW_KEY_KP_6:
				case GLFW.GLFW_KEY_6:
					client.getPlayer().setSlot(5);
					break;
				case GLFW.GLFW_KEY_KP_7:
				case GLFW.GLFW_KEY_7:
					client.getPlayer().setSlot(6);
					break;
				case GLFW.GLFW_KEY_KP_8:
				case GLFW.GLFW_KEY_8:
					client.getPlayer().setSlot(7);
					break;
			    case GLFW.GLFW_KEY_KP_9:
				case GLFW.GLFW_KEY_9:
					client.getPlayer().setSlot(8);
					break;
			}	
		}
	}
}