package ritzow.sandbox.client.util;

import ritzow.sandbox.client.world.block.ClientGlassBlock;
import ritzow.sandbox.client.world.block.ClientDirtBlock;
import ritzow.sandbox.client.world.block.ClientGrassBlock;
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
			.registerRead(Protocol.DATA_BLOCK_GRID, BlockGrid::new)
			.registerRead(Protocol.DATA_WORLD, World::new)
			.registerRead(Protocol.DATA_BLOCK_ITEM, ClientBlockItem::new)
			.registerRead(Protocol.DATA_DIRT_BLOCK, ClientDirtBlock::getSingleton)
			.registerRead(Protocol.DATA_GRASS_BLOCK, ClientGrassBlock::getSingleton)
			.registerRead(Protocol.DATA_GLASS_BLOCK, ClientGlassBlock::getSingleton)
			.registerRead(Protocol.DATA_PLAYER_ENTITY, ClientPlayerEntity::new)
			.registerRead(Protocol.DATA_INVENTORY, Inventory::new)
			.registerRead(Protocol.DATA_ITEM_ENTITY, ClientItemEntity::new);

	public static SerializerReaderWriter getProvider() {
		return provider;
	}
}
