package ritzow.sandbox.client.input.controller;

import java.time.Instant;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.input.Control;
import ritzow.sandbox.client.input.ControlsContext;
import ritzow.sandbox.client.network.GameTalker;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
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

//	public void render(Display display, ModelRenderer renderer, ControlsQuery controls,
//				Camera camera, World world, ClientPlayerEntity player) {
//		int width = display.width(), height = display.height();
//		int blockX = Math.round(ClientUtility.pixelHorizontalToWorld(
//				camera, display.getCursorX(), width, height));
//		int blockY = Math.round(ClientUtility.pixelVerticalToWorld(
//				camera, display.getCursorY(), height));
//		renderer.loadViewMatrix(camera, width, height);
//		switch(player.selected()) {
//			case Protocol.SLOT_PLACE_GRASS -> renderToolOvelay(world, renderer, player, GameModels.MODEL_GRASS_BLOCK, blockX, blockY);
//			case Protocol.SLOT_PLACE_DIRT -> renderToolOvelay(world, renderer, player, GameModels.MODEL_DIRT_BLOCK, blockX, blockY);
//			case Protocol.SLOT_BREAK -> renderer.queueRender(
//				GameModels.MODEL_RED_SQUARE,
//				1.0f,
//				//computeOpacity(Utility.canBreak(player, world, blockX, blockY) && Utility.isBreakable(world.getBlocks(), layer, blockX, blockY)),
//				layer * ClientWorldRenderer.LAYER_EXPOSURE_FACTOR,
//				blockX, blockY, 1.0f, 1.0f, 0.0f
//			);
//		}
//	}

	//!world.getBlocks().isValid(World.LAYER_MAIN, blockX, blockY) || !world.getBlocks().isBlock(World.LAYER_MAIN, blockX, blockY)

	//refactor this a bit, very confusing
	private static void renderToolOvelay(ModelRenderer renderer, Model model, int blockX, int blockY, boolean useable) {
		renderer.queueRender(
			model,
			useable ? Utility.oscillate(ACTIVATE_INDICATOR_SPEED, 0, 0.5f, 1.0f) : 0.25f,
			1.0f,
			blockX, blockY, 1.0f, 1.0f, 0.0f
		);
	}

	public void update(Display display, ControlsContext controls, ModelRenderer renderer, Camera camera, GameTalker client, World world, PlayerEntity player) {
		int framebufferHeight = display.height();
		int blockX = Math.round(ClientUtility.pixelHorizontalToWorld(camera, display.getCursorX(), display.width(), framebufferHeight));
		int blockY = Math.round(ClientUtility.pixelVerticalToWorld(camera, display.getCursorY(), framebufferHeight));
		BlockGrid blocks = world.getBlocks();
		switch(player.selected()) {
			case Protocol.SLOT_BREAK -> {
				if(blocks.isValid(blockX, blockY)) {
					boolean isPressed = controls.isPressed(Control.USE_HELD_ITEM);
					int hoveredLayer = Utility.getBlockBreakLayer(blocks, blockX, blockY);
					boolean isCorrectLayer;
					if(!isPressed || layer < 0) {
						layer = hoveredLayer;
						isCorrectLayer = true;
					} else {
						isCorrectLayer = layer == hoveredLayer;
					}

					boolean useable = layer >= 0 && isCorrectLayer &&
						Instant.now().isAfter(nextUseTime) &&
						Utility.isBreakable(blocks, layer, blockX, blockY);

					renderToolOvelay(renderer, GameModels.MODEL_RED_SQUARE, blockX, blockY, useable);

					if(useable && isPressed) {
						client.sendBlockBreak(blockX, blockY);
						nextUseTime = Instant.MAX;
					}
				} else {
					renderToolOvelay(renderer, GameModels.MODEL_RED_SQUARE, blockX, blockY, false);
				}
			}

			case Protocol.SLOT_PLACE_GRASS, Protocol.SLOT_PLACE_DIRT, Protocol.SLOT_PLACE_GLASS -> {
				if(blocks.isValid(blockX, blockY)) {
					boolean isPressed = controls.isPressed(Control.USE_HELD_ITEM);
					if(!isPressed || layer < 0) {
						layer = Utility.getBlockPlaceLayer(blocks, blockX, blockY);
					}

					boolean useable = layer >= 0 && Instant.now().isAfter(nextUseTime) &&
		                Utility.canPlace(player, world, blockX, blockY) &&
		                Utility.isPlaceable(blocks, layer, blockX, blockY);

					renderToolOvelay(renderer, switch(player.selected()) {
						case Protocol.SLOT_PLACE_DIRT -> GameModels.MODEL_DIRT_BLOCK;
						case Protocol.SLOT_PLACE_GRASS -> GameModels.MODEL_GRASS_BLOCK;
						case Protocol.SLOT_PLACE_GLASS -> GameModels.MODEL_GLASS_BLOCK;
						default -> throw new IllegalArgumentException();
					}, blockX, blockY, useable);

					if(useable && isPressed) {
						client.sendBlockPlace(blockX, blockY);
						nextUseTime = Instant.MAX;
					}
				} else {
					renderToolOvelay(renderer, switch(player.selected()) {
						case Protocol.SLOT_PLACE_DIRT -> GameModels.MODEL_DIRT_BLOCK;
						case Protocol.SLOT_PLACE_GRASS -> GameModels.MODEL_GRASS_BLOCK;
						case Protocol.SLOT_PLACE_GLASS -> GameModels.MODEL_GLASS_BLOCK;
						default -> throw new IllegalArgumentException();
					}, blockX, blockY, false);
				}
			}
		}
	}
}