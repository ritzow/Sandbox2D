package ritzow.sandbox.client.input.controller;

import java.time.Instant;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.ControlsContext;
import ritzow.sandbox.client.input.ControlsQuery;
import ritzow.sandbox.client.network.GameTalker;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.client.world.ClientWorldRenderer;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.PlayerEntity;

public final class InteractionController {
	private static final double ACTIVATE_INDICATOR_SPEED = Utility.degreesPerSecToRadiansPerNano(240);
	private Instant nextUseTime;
	private int layer;

	public InteractionController() {
		nextUseTime = Instant.MIN;
	}

	public void setNextUseTime(Instant time) {
		this.nextUseTime = time;
	}

	public void render(Display display, ModelRenderer renderer, ControlsQuery controls,
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
			case Protocol.SLOT_BREAK -> renderer.queueRender(
				GameModels.MODEL_RED_SQUARE,
				1.0f,
				//computeOpacity(Utility.canBreak(player, world, blockX, blockY) && Utility.isBreakable(world.getBlocks(), layer, blockX, blockY)),
				layer * ClientWorldRenderer.LAYER_EXPOSURE_FACTOR,
				blockX, blockY, 1.0f, 1.0f, 0.0f
			);
		}
	}

	//refactor this a bit, very confusing
	private void renderToolOvelay(World world, ModelRenderer renderer, PlayerEntity player, Model model, int blockX, int blockY) {
		if(!world.getBlocks().isValid(World.LAYER_MAIN, blockX, blockY) || !world.getBlocks().isBlock(World.LAYER_MAIN, blockX, blockY)) {
			renderer.queueRender(
				model,
				computeOpacity(Utility.canPlace(player, world, blockX, blockY)),
				layer * ClientWorldRenderer.LAYER_EXPOSURE_FACTOR,
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

					if(Instant.now().isAfter(nextUseTime) && Utility.getBlockBreakLayer(world.getBlocks(), blockX, blockY) == layer) {
						if(Utility.isBreakable(world.getBlocks(), layer, blockX, blockY)) {
							client.sendBlockBreak(blockX, blockY);
							nextUseTime = Instant.MAX;
							//requires a different approach, lastBreak won't be set here
						}
					}
				}

				case Protocol.SLOT_PLACE_GRASS, Protocol.SLOT_PLACE_DIRT -> {
					if(controls.isNewlyPressed(Control.USE_HELD_ITEM) || layer < 0) {
						//TODO this won't work if the network ping is more than a single frame duration.
						layer = Utility.getBlockPlaceLayer(world.getBlocks(), blockX, blockY);
					}

					if(Instant.now().isAfter(nextUseTime) && Utility.canPlace(player, world, blockX, blockY)) {
						if(Utility.isPlaceable(world.getBlocks(), layer, blockX, blockY)) {
							client.sendBlockPlace(blockX, blockY);
							nextUseTime = Instant.MAX;
						}
					}
				}
			}
		}
	}
}