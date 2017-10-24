package ritzow.sandbox.server;

import ritzow.sandbox.data.SerializerReaderWriter;
import ritzow.sandbox.network.Protocol;
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
		provider.register(Protocol.BLOCK_GRID, BlockGrid.class, BlockGrid::new);
		provider.register(Protocol.WORLD, World.class, World::new);
		provider.register(Protocol.BLOCK_ITEM, BlockItem.class, BlockItem::new);
		provider.register(Protocol.DIRT_BLOCK, DirtBlock.class, DirtBlock::new);
		provider.register(Protocol.GRASS_BLOCK, GrassBlock.class, GrassBlock::new);
		provider.register(Protocol.PLAYER_ENTITY, PlayerEntity.class, PlayerEntity::new);
		provider.register(Protocol.INVENTORY, Inventory.class, Inventory::new);
		provider.register(Protocol.ITEM_ENTITY, ItemEntity.class, ItemEntity::new);
	}
	
	public static SerializerReaderWriter getProvider() {
		return provider;
	}
}
