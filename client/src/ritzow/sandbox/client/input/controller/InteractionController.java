package ritzow.sandbox.client.input.controller;

import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.ControlsContext;
import ritzow.sandbox.client.network.GameTalker;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.PlayerEntity;

public final class InteractionController {
	private static final double ACTIVATE_INDICATOR_SPEED = Utility.degreesPerSecToRadiansPerNano(240);

	private long lastThrow, lastBreak, lastPlace;
	private int layer;

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
			case Protocol.SLOT_PLACE_GRASS -> renderToolOvelay(world, renderer, player, GameModels.MODEL_GRASS_BLOCK, blockX, blockY);
			case Protocol.SLOT_PLACE_DIRT -> renderToolOvelay(world, renderer, player, GameModels.MODEL_DIRT_BLOCK, blockX, blockY);
			default -> renderer.queueRender(
				GameModels.MODEL_RED_SQUARE,
				computeOpacity(Utility.canBreak(player, lastPlace, world, blockX, blockY)),
				blockX, blockY, 1.0f, 1.0f, 0.0f
			);
		}
	}

	//refactor this a bit, very confusing
	private void renderToolOvelay(World world, ModelRenderer renderer, PlayerEntity player, Model model, int blockX, int blockY) {
		if(!world.getBlocks().isValid(World.LAYER_MAIN, blockX, blockY) || !world.getBlocks().isBlock(World.LAYER_MAIN, blockX, blockY)) {
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
		if(controls.isPressed(Control.USE_HELD_ITEM)) {
			int blockX = Math.round(ClientUtility.pixelHorizontalToWorld(camera, display.getCursorX(), display.width(), display.height()));
			int blockY = Math.round(ClientUtility.pixelVerticalToWorld(camera, display.getCursorY(), display.height()));
			switch(player.selected()) {
				case Protocol.SLOT_BREAK -> {
					if(controls.isNewlyPressed(Control.USE_HELD_ITEM) || layer < 0) {
						//TODO this won't work if the network ping is more than a single frame duration.
						layer = Utility.getBlockBreakLayer(world.getBlocks(), blockX, blockY);
					}

					if(Utility.canBreak(player, lastBreak, world, blockX, blockY)) {
						if(Utility.isBreakable(world.getBlocks(), layer, blockX, blockY)) {
							client.sendBlockBreak(blockX, blockY);
							//TODO comm. with server, only reset cooldown to server provided value if a block is actually broken
							//requires a different approach, lastBreak won't be set here
							lastBreak = System.nanoTime();
						}
					}
				}

				case Protocol.SLOT_PLACE_GRASS, Protocol.SLOT_PLACE_DIRT -> {
					if(controls.isNewlyPressed(Control.USE_HELD_ITEM) || layer < 0) {
						//TODO this won't work if the network ping is more than a single frame duration.
						layer = Utility.getBlockPlaceLayer(world.getBlocks(), blockX, blockY);
					}

					if(Utility.canPlace(player, lastPlace, world, blockX, blockY)) {
						if(Utility.isPlaceable(world.getBlocks(), layer, blockX, blockY)) {
							client.sendBlockPlace(blockX, blockY);
							lastPlace = System.nanoTime();
							//TODO comm. with server, only reset cooldown to server provided value if a block is actually broken
						}
					}
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