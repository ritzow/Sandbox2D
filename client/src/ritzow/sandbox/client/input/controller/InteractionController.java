package ritzow.sandbox.client.input.controller;

import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.graphics.Display;
import ritzow.sandbox.client.graphics.ModelRenderProgram;
import ritzow.sandbox.client.graphics.RenderConstants;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.network.GameTalker;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.PlayerEntity;

public final class InteractionController {
	private long lastThrow, lastBreak, lastPlace;
	
	public void render(Display display, ModelRenderProgram renderer, 
			Camera camera, BlockGrid grid, ClientPlayerEntity player) {
		int modelID = switch(player.selected()) {
			case 1 -> RenderConstants.MODEL_GRASS_BLOCK;
			case 2 -> RenderConstants.MODEL_DIRT_BLOCK;
			default -> -1;
		};
		int width = display.width(), height = display.height();
		int blockX = Math.round(ClientUtility.pixelHorizontalToWorld(
				camera, display.getCursorX(), width, height));
		int blockY = Math.round(ClientUtility.pixelVerticalToWorld(
				camera, display.getCursorY(), height));
		renderer.loadViewMatrix(camera, width, height);
		if(modelID != -1) {
			renderer.render(
				modelID,
				Utility.canPlace(player, lastPlace, grid, blockX, blockY) ? 0.75f : 0.25f,
				blockX, blockY, 1.0f, 1.0f, 0.0f
			);	
		} else if(Utility.canBreak(player, lastBreak, grid, blockX, blockY)) {
			renderer.render(
				RenderConstants.MODEL_RED_SQUARE,
				0.5f, blockX, blockY, 1.0f, 1.0f, 0.0f
			);
		}
	}

	//TODO wait for server response before sending more block break packets
	public void update(Display display, Camera camera, GameTalker client, World world, PlayerEntity player) {
		final int mouseX = display.getCursorX(), mouseY = display.getCursorY();
		final int frameWidth = display.width(), frameHeight = display.height();
		if(display.isControlActivated(Control.USE_HELD_ITEM)) {
			int blockX = Math.round(ClientUtility.pixelHorizontalToWorld(camera, mouseX, frameWidth, frameHeight));
			int blockY = Math.round(ClientUtility.pixelVerticalToWorld(camera, mouseY, frameHeight));
			BlockGrid grid = world.getForeground();
			if(player.selected() == 0) {
				if(Utility.canBreak(player, lastBreak, grid, blockX, blockY)) {
					client.sendBlockBreak(blockX, blockY);
					//TODO comm. with server, only reset cooldown to server provided value if a block is actually broken
					//requires a different approach, lastBreak won't be set here
					lastBreak = System.nanoTime();
				}
			} else {
				if(Utility.canPlace(player, lastPlace, grid, blockX, blockY)) {
					client.sendBlockPlace(blockX, blockY);
					lastPlace = System.nanoTime();
					//TODO comm. with server, only reset cooldown to server provided value if a block is actually broken
				}
			}
		}
		
		if(display.isControlActivated(Control.THROW_BOMB) && Utility.canThrow(lastThrow)) {
			float worldX = ClientUtility.pixelHorizontalToWorld(camera, mouseX, frameWidth, frameHeight);
			float worldY = ClientUtility.pixelVerticalToWorld(camera, mouseY, frameHeight);
			client.sendBombThrow(computeThrowAngle(player, worldX, worldY));
			lastThrow = System.nanoTime();
		}
	}

	private static float computeThrowAngle(Entity player, float worldX, float worldY) {
		return (float)Math.atan2(worldY - player.getPositionY(), worldX - player.getPositionX())
				+ Utility.random(-Math.PI/8, Math.PI/8);
	}
}