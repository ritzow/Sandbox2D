package ritzow.sandbox.client.input.controller;

import static org.lwjgl.glfw.GLFW.*;

import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.input.EventDelegator;
import ritzow.sandbox.client.input.handler.CursorPosHandler;
import ritzow.sandbox.client.input.handler.KeyHandler;
import ritzow.sandbox.client.input.handler.MouseButtonHandler;
import ritzow.sandbox.client.network.Client;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;

public final class InteractionController implements MouseButtonHandler, CursorPosHandler, KeyHandler {
	private volatile boolean primaryAction, secondaryAction;
	private volatile int mouseX, mouseY;
	private long lastThrow, lastBreak;
	private float range;
	private final Client client;

	public InteractionController(Client client, float range) {
		this.client = client;
		this.range = range;
	}

	//TODO wait for server response before sending more block break packets
	public void update(Camera camera, Client client, World world, Entity player, int frameWidth, int frameHeight) {
		if(primaryAction && breakAllowed()) {
			int blockX = Math.round(ClientUtility.pixelHorizontalToWorld(camera, mouseX, frameWidth, frameHeight));
			int blockY = Math.round(ClientUtility.pixelVerticalToWorld(camera, mouseY, frameWidth, frameHeight));
			BlockGrid grid = world.getForeground();
			if(inRange(player, blockX, blockY) && grid.isValid(blockX, blockY) && grid.isBlock(blockX, blockY)) {
				client.sendBlockBreak(blockX, blockY);
				lastBreak = System.nanoTime(); //TODO only reset cooldown if a block is actually broken
			}
		} else if(secondaryAction && throwAllowed()) {
			float worldX = ClientUtility.pixelHorizontalToWorld(camera, mouseX, frameWidth, frameHeight);
			float worldY = ClientUtility.pixelVerticalToWorld(camera, mouseY, frameWidth, frameHeight);
			client.sendBombThrow((float)Math.atan2(worldY - player.getPositionY(), worldX - player.getPositionX()) + Utility.randomFloat(-(float)Math.PI/8, (float)Math.PI/8));
			lastThrow = System.nanoTime();
		}
	}

	private boolean inRange(Entity player, int blockX, int blockY) {
		return Utility.withinDistance(player.getPositionX(), player.getPositionY(), blockX, blockY, range);
	}

	private boolean breakAllowed() {
		return Utility.nanosSince(lastBreak) > Protocol.BLOCK_BREAK_COOLDOWN_NANOSECONDS;
	}

	private boolean throwAllowed() {
		return Utility.nanosSince(lastThrow) > Protocol.THROW_COOLDOWN_NANOSECONDS;
	}

	public void link(EventDelegator input) {
		input.cursorPosHandlers().add(this);
		input.keyboardHandlers().add(this);
		input.mouseButtonHandlers().add(this);
	}

	public void unlink(EventDelegator input) {
		input.cursorPosHandlers().remove(this);
		input.keyboardHandlers().remove(this);
		input.mouseButtonHandlers().remove(this);
	}

	@Override
	public void mouseButton(int button, int action, int mods) {
		primaryAction = ((button == GLFW_MOUSE_BUTTON_LEFT || primaryAction) && action == GLFW_PRESS);
		secondaryAction = ((button == GLFW_MOUSE_BUTTON_RIGHT || secondaryAction) && action == GLFW_PRESS);
	}

	@Override
	public void cursorPos(double xpos, double ypos) {
		mouseX = (int) xpos;
		mouseY = (int) ypos;
	}

	@Override
	public void keyboardButton(int key, int scancode, int action, int mods) { //TODO using callbacks is stupid in singlethreaded, check keyboard buttons in update instead.
		try {
			if(action == GLFW_PRESS) {
				switch(key) {
					case GLFW_KEY_KP_1:
					case GLFW_KEY_1:
						client.getPlayer().setSlot(0);
						break;
					case GLFW_KEY_KP_2:
					case GLFW_KEY_2:
						client.getPlayer().setSlot(1);
						break;
					case GLFW_KEY_KP_3:
					case GLFW_KEY_3:
						client.getPlayer().setSlot(2);
						break;
					case GLFW_KEY_KP_4:
					case GLFW_KEY_4:
						client.getPlayer().setSlot(3);
						break;
					case GLFW_KEY_KP_5:
					case GLFW_KEY_5:
						client.getPlayer().setSlot(4);
						break;
				    case GLFW_KEY_KP_6:
					case GLFW_KEY_6:
						client.getPlayer().setSlot(5);
						break;
					case GLFW_KEY_KP_7:
					case GLFW_KEY_7:
						client.getPlayer().setSlot(6);
						break;
					case GLFW_KEY_KP_8:
					case GLFW_KEY_8:
						client.getPlayer().setSlot(7);
						break;
				    case GLFW_KEY_KP_9:
					case GLFW_KEY_9:
						client.getPlayer().setSlot(8);
						break;
				}
			}
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
}