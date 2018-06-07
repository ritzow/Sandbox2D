package ritzow.sandbox.client.util;

import ritzow.sandbox.client.world.block.ClientDirtBlock;
import ritzow.sandbox.client.world.block.ClientGrassBlock;
import ritzow.sandbox.client.world.entity.ClientBombEntity;
import ritzow.sandbox.client.world.entity.ClientItemEntity;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.client.world.item.ClientBlockItem;
import ritzow.sandbox.data.SerializerReaderWriter;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.component.Inventory;

public final class SerializationProvider {
	private static final SerializerReaderWriter provider = new SerializerReaderWriter()
			.register(Protocol.BLOCK_GRID, BlockGrid.class, BlockGrid::new)
			.register(Protocol.WORLD, World.class, World::new)
			.register(Protocol.BLOCK_ITEM, ClientBlockItem.class, ClientBlockItem::new)
			.register(Protocol.DIRT_BLOCK, ClientDirtBlock.class, ClientDirtBlock::new)
			.register(Protocol.GRASS_BLOCK, ClientGrassBlock.class, ClientGrassBlock::new)
			.register(Protocol.PLAYER_ENTITY, ClientPlayerEntity.class, ClientPlayerEntity::new)
			.register(Protocol.INVENTORY, Inventory.class, Inventory::new)
			.register(Protocol.ITEM_ENTITY, ClientItemEntity.class, ClientItemEntity::new)
			.register(Protocol.BOMB_ENTITY, ClientBombEntity.class, ClientBombEntity::new);
	
	public static SerializerReaderWriter getProvider() {
		return provider;
	}
}
