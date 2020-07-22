package ritzow.sandbox.client.input.controller;

import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.ControlsContext;
import ritzow.sandbox.client.network.GameTalker;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.PlayerEntity;

public final class InteractionController {
	private static final double ACTIVATE_INDICATOR_SPEED = Utility.degreesPerSecToRadiansPerNano(240);

	private long lastThrow, lastBreak, lastPlace;

	public void setLastBreak(long time) {
		this.lastBreak = time;
	}

	public void render(Display display, ModelRenderer renderer,
				Camera camera, World world, ClientPlayerEntity player) {
		int width = display.width(), height = display.height();
		int blockX = Math.round(ClientUtility.pixelHorizontalToWorld(
				camera, display.getCursorX(), width, height));
		int blockY = Math.round(ClientUtility.pixelVerticalToWorld(
				camera, display.getCursorY(), height));
		renderer.loadViewMatrix(camera, width, height);
		switch(player.selected()) {
			case 1 -> renderToolOvelay(world, renderer, player, GameModels.MODEL_GRASS_BLOCK, blockX, blockY);
			case 2 -> renderToolOvelay(world, renderer, player, GameModels.MODEL_DIRT_BLOCK, blockX, blockY);
			default -> renderer.queueRender(
				GameModels.MODEL_RED_SQUARE,
				computeOpacity(Utility.canBreak(player, lastPlace, world, blockX, blockY)),
				blockX, blockY, 1.0f, 1.0f, 0.0f
			);
		}
	}

	private void renderToolOvelay(World world, ModelRenderer renderer, PlayerEntity player, Model model, int blockX, int blockY) {
		if(!world.getForeground().isValid(blockX, blockY) || !world.getForeground().isBlock(blockX, blockY)) {
			renderer.queueRender(
				model,
				computeOpacity(Utility.canPlace(player, lastPlace, world, blockX, blockY)),
				blockX, blockY, 1.0f, 1.0f, 0.0f
			);
		}
	}

	private static float computeOpacity(boolean active) {
		return active ? Utility.oscillate(ACTIVATE_INDICATOR_SPEED, 0, 0.5f, 1.0f) : 0.25f;
	}

	//TODO wait for server to ack that block place/break cooldown has expired before sending more
	public void update(Display display, ControlsContext controls, Camera camera, GameTalker client, World world, PlayerEntity player) {
		final int mouseX = display.getCursorX(), mouseY = display.getCursorY();
		final int frameWidth = display.width(), frameHeight = display.height();
		if(controls.isPressed(Control.USE_HELD_ITEM)) {
			int blockX = Math.round(ClientUtility.pixelHorizontalToWorld(camera, mouseX, frameWidth, frameHeight));
			int blockY = Math.round(ClientUtility.pixelVerticalToWorld(camera, mouseY, frameHeight));
			if(player.selected() == 0) {
				if(Utility.canBreak(player, lastBreak, world, blockX, blockY)) {
					client.sendBlockBreak(blockX, blockY);
					//TODO comm. with server, only reset cooldown to server provided value if a block is actually broken
					//requires a different approach, lastBreak won't be set here
					lastBreak = System.nanoTime();
				}
			} else {
				if(Utility.canPlace(player, lastPlace, world, blockX, blockY)) {
					client.sendBlockPlace(blockX, blockY);
					lastPlace = System.nanoTime();
					//TODO comm. with server, only reset cooldown to server provided value if a block is actually broken
				}
			}
		}

		if(controls.isNewlyPressed(Control.THROW_BOMB) && Utility.canThrow(lastThrow)) {
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