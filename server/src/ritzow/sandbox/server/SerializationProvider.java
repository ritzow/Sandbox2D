package ritzow.sandbox.server;

import ritzow.sandbox.data.SerializerReaderWriter;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.server.world.entity.ServerPlayerEntity;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.DirtBlock;
import ritzow.sandbox.world.block.GlassBlock;
import ritzow.sandbox.world.block.GrassBlock;
import ritzow.sandbox.world.component.Inventory;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.item.BlockItem;

public class SerializationProvider {
	private static final SerializerReaderWriter provider = new SerializerReaderWriter()
			.register(Protocol.DATA_WORLD, World.class, World::new)
			.register(Protocol.DATA_BLOCK_GRID, BlockGrid.class, BlockGrid::new)
			.register(Protocol.DATA_BLOCK_ITEM, BlockItem.class, BlockItem::new)
			.register(Protocol.DATA_DIRT_BLOCK, DirtBlock.class, reader -> DirtBlock.INSTANCE)
			.register(Protocol.DATA_GRASS_BLOCK, GrassBlock.class, reader -> GrassBlock.INSTANCE)
			.register(Protocol.DATA_GLASS_BLOCK, GlassBlock.class, reader -> GlassBlock.INSTANCE)
			.register(Protocol.DATA_ITEM_ENTITY, ItemEntity.class, ItemEntity::new)
			.registerWrite(Protocol.DATA_INVENTORY, Inventory.class)
			.registerWrite(Protocol.DATA_PLAYER_ENTITY, ServerPlayerEntity.class);

	public static SerializerReaderWriter getProvider() {
		return provider;
	}
}
