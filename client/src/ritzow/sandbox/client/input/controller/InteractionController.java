package ritzow.sandbox.client.input.controller;

import org.lwjgl.glfw.GLFW;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.input.EventDelegator;
import ritzow.sandbox.client.input.handler.CursorPosHandler;
import ritzow.sandbox.client.input.handler.FramebufferSizeHandler;
import ritzow.sandbox.client.input.handler.KeyHandler;
import ritzow.sandbox.client.input.handler.MouseButtonHandler;
import ritzow.sandbox.client.network.Client;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;

public final class InteractionController implements Controller, MouseButtonHandler, CursorPosHandler, FramebufferSizeHandler, KeyHandler {
	private volatile boolean primaryAction, secondaryAction;
	private volatile int frameWidth, frameHeight;
	private volatile int mouseX, mouseY;
	private long lastPlace, lastBreak;
	private float range;
	private final Camera camera;
	private final Client client;
	
	private static final long COOLDOWN_THROW = Utility.millisToNanos(0), COOLDOWN_BREAK = Utility.millisToNanos(200);
	
	public InteractionController(Client client, Camera camera, float range) {
		this.client = client;
		this.camera = camera;
		this.range = range;
	}
	
	@Override
	public void update() {
		update(camera, frameWidth, frameHeight);
	}
	
	protected void update(Camera camera, int frameWidth, int frameHeight) {
		if(primaryAction && breakAllowed()) {
			int blockX = Math.round(ClientUtility.pixelHorizontalToWorld(camera, mouseX, frameWidth, frameHeight));
			int blockY = Math.round(ClientUtility.pixelVerticalToWorld(camera, mouseY, frameWidth, frameHeight));
			ClientPlayerEntity player = client.getPlayer();
			BlockGrid front = client.getWorld().getForeground();
			if(front.isValid(blockX, blockY) && inRange(player, blockX, blockY) && front.isBlock(blockX, blockY)) {
				client.sendBlockBreak(blockX, blockY);
				lastBreak = System.nanoTime();
			}
		} else if(secondaryAction && throwAllowed()) {
			float worldX = ClientUtility.pixelHorizontalToWorld(camera, mouseX, frameWidth, frameHeight);
			float worldY = ClientUtility.pixelVerticalToWorld(camera, mouseY, frameWidth, frameHeight);
			ClientPlayerEntity player = client.getPlayer();
			client.sendBombThrow((float)Math.atan2(worldY - player.getPositionY(), worldX - player.getPositionX()) + Utility.randomFloat(-(float)Math.PI/8, (float)Math.PI/8));
			lastPlace = System.nanoTime();
		}
	}
	
	private boolean inRange(ClientPlayerEntity player, int blockX, int blockY) {
		return Utility.withinDistance(player.getPositionX(), player.getPositionY(), blockX, blockY, range);
	}
	
	private boolean breakAllowed() {
		return Utility.nanosSince(lastBreak) > COOLDOWN_BREAK;
	}
	
	private boolean throwAllowed() {
		return Utility.nanosSince(lastPlace) > COOLDOWN_THROW;
	}

	public void link(EventDelegator input) {
		input.cursorPosHandlers().add(this);
		input.framebufferSizeHandlers().add(this);
		input.keyboardHandlers().add(this);
		input.mouseButtonHandlers().add(this);
	}
	
	public void unlink(EventDelegator input) {
		input.cursorPosHandlers().remove(this);
		input.framebufferSizeHandlers().remove(this);
		input.keyboardHandlers().remove(this);
		input.mouseButtonHandlers().remove(this);
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