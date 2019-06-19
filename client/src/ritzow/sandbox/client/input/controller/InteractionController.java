package ritzow.sandbox.client.input.controller;

import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.InputProvider;
import ritzow.sandbox.client.network.GameTalker;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;

public final class InteractionController {
	private long lastThrow, lastBreak;

	//TODO wait for server response before sending more block break packets
	public void update(InputProvider input, Camera camera, GameTalker client, World world, Entity player, int frameWidth, int frameHeight) {
		final int mouseX = input.getCursorX(), mouseY = input.getCursorY();
		if(input.isControlActivated(Control.USE_HELD_ITEM) && breakAllowed()) {
			int blockX = Math.round(ClientUtility.pixelHorizontalToWorld(camera, mouseX, frameWidth, frameHeight));
			int blockY = Math.round(ClientUtility.pixelVerticalToWorld(camera, mouseY, frameHeight));
			BlockGrid grid = world.getForeground();
			if(inRange(player, blockX, blockY) && grid.isValid(blockX, blockY) && grid.isBlock(blockX, blockY)) {
				client.sendBlockBreak(blockX, blockY);
				//TODO comm. with server, only reset cooldown to server provided value if a block is actually broken
				//requires a different approach, lastBreak won't be set here
				lastBreak = System.nanoTime();
			}
		} else if(input.isControlActivated(Control.THROW_BOMB) && throwAllowed()) {
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
}