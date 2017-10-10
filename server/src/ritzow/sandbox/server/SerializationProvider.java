package ritzow.sandbox.server;

import ritzow.sandbox.data.SerializerReaderWriter;
import ritzow.sandbox.network.WorldObjectIdentifiers;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.DirtBlock;
import ritzow.sandbox.world.block.GrassBlock;
import ritzow.sandbox.world.component.Inventory;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.entity.PlayerEntity;
import ritzow.sandbox.world.item.BlockItem;

public class SerializationProvider {
	private static final SerializerReaderWriter provider;
	
	static {
		provider = new SerializerReaderWriter();
		provider.register(WorldObjectIdentifiers.BLOCK_GRID, BlockGrid.class, BlockGrid::new);
		provider.register(WorldObjectIdentifiers.WORLD, World.class, World::new);
		provider.register(WorldObjectIdentifiers.BLOCK_ITEM, BlockItem.class, BlockItem::new);
		provider.register(WorldObjectIdentifiers.DIRT_BLOCK, DirtBlock.class, DirtBlock::new);
		provider.register(WorldObjectIdentifiers.GRASS_BLOCK, GrassBlock.class, GrassBlock::new);
		provider.register(WorldObjectIdentifiers.PLAYER_ENTITY, PlayerEntity.class, PlayerEntity::new);
		provider.register(WorldObjectIdentifiers.INVENTORY, Inventory.class, Inventory::new);
		provider.register(WorldObjectIdentifiers.ITEM_ENTITY, ItemEntity.class, ItemEntity::new);
	}
	
	public static SerializerReaderWriter getProvider() {
		return provider;
	}
}
