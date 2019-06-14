package ritzow.sandbox.client.input.controller;

import static org.lwjgl.glfw.GLFW.*;

import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.input.handler.CursorPosHandler;
import ritzow.sandbox.client.input.handler.KeyHandler;
import ritzow.sandbox.client.input.handler.MouseButtonHandler;
import ritzow.sandbox.client.network.GameTalker;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;

public final class InteractionController implements MouseButtonHandler, CursorPosHandler, KeyHandler {
	private boolean primaryAction, secondaryAction;
	private int mouseX, mouseY;
	private long lastThrow, lastBreak;
	private final GameTalker client;

	public InteractionController(GameTalker client) {
		this.client = client;
	}

	//TODO wait for server response before sending more block break packets
	public void update(Camera camera, GameTalker client, World world, Entity player, int frameWidth, int frameHeight) {
		if(primaryAction && breakAllowed()) {
			int blockX = Math.round(ClientUtility.pixelHorizontalToWorld(camera, mouseX, frameWidth, frameHeight));
			int blockY = Math.round(ClientUtility.pixelVerticalToWorld(camera, mouseY, frameHeight));
			BlockGrid grid = world.getForeground();
			if(inRange(player, blockX, blockY) && grid.isValid(blockX, blockY) && grid.isBlock(blockX, blockY)) {
				client.sendBlockBreak(blockX, blockY);
				//TODO comm. with server, only reset cooldown to server provided value if a block is actually broken
				//requires a different approach, lastBreak won't be set here
				lastBreak = System.nanoTime();
			}
		} else if(secondaryAction && throwAllowed()) {
			float worldX = ClientUtility.pixelHorizontalToWorld(camera, mouseX, frameWidth, frameHeight);
			float worldY = ClientUtility.pixelVerticalToWorld(camera, mouseY, frameHeight);
			client.sendBombThrow(computeThrowAngle(player, worldX, worldY));
			lastThrow = System.nanoTime();
		}
	}
	
	private static final float MAX_ANGLE = (float)Math.PI/8;
	
	private static float computeThrowAngle(Entity player, float worldX, float worldY) {
		return (float)Math.atan2(worldY - player.getPositionY(), worldX - player.getPositionX()) 
				+ Utility.randomFloat(-MAX_ANGLE, MAX_ANGLE);
	}

	private static boolean inRange(Entity player, int blockX, int blockY) {
		return Utility.withinDistance(player.getPositionX(), player.getPositionY(), 
				blockX, blockY, Protocol.BLOCK_BREAK_RANGE);
	}

	private boolean breakAllowed() {
		return Utility.nanosSince(lastBreak) > Protocol.BLOCK_BREAK_COOLDOWN_NANOSECONDS;
	}

	private boolean throwAllowed() {
		return Utility.nanosSince(lastThrow) > Protocol.THROW_COOLDOWN_NANOSECONDS;
	}

	@Override
	public void mouseButton(int button, int action, int mods) {
		primaryAction = action == GLFW_PRESS && (button == GLFW_MOUSE_BUTTON_LEFT || primaryAction);
		secondaryAction = action == GLFW_PRESS && (button == GLFW_MOUSE_BUTTON_RIGHT || secondaryAction);
	}

	@Override
	public void cursorPos(double xpos, double ypos) {
		mouseX = (int) xpos;
		mouseY = (int) ypos;
	}

	//TODO using callbacks is stupid in singlethreaded, check keyboard buttons in update instead.
	@Override
	public void keyboardButton(int key, int scancode, int action, int mods) {
		if(action == GLFW_PRESS) {
			switch(key) {
				case GLFW_KEY_1, GLFW_KEY_KP_1 -> client.getPlayer().setSlot(0);
				case GLFW_KEY_2, GLFW_KEY_KP_2 -> client.getPlayer().setSlot(1);
				case GLFW_KEY_3, GLFW_KEY_KP_3 -> client.getPlayer().setSlot(2);
				case GLFW_KEY_4, GLFW_KEY_KP_4 -> client.getPlayer().setSlot(3);
				case GLFW_KEY_5, GLFW_KEY_KP_5 -> client.getPlayer().setSlot(4);
			    case GLFW_KEY_6, GLFW_KEY_KP_6 -> client.getPlayer().setSlot(5);
				case GLFW_KEY_7, GLFW_KEY_KP_7 -> client.getPlayer().setSlot(6);
				case GLFW_KEY_8, GLFW_KEY_KP_8 -> client.getPlayer().setSlot(7);
			    case GLFW_KEY_9, GLFW_KEY_KP_9 -> client.getPlayer().setSlot(8);
			}
		}
	}
}