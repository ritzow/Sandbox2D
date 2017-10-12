package ritzow.sandbox.client.util;

import ritzow.sandbox.client.world.block.ClientDirtBlock;
import ritzow.sandbox.client.world.block.ClientGrassBlock;
import ritzow.sandbox.client.world.entity.ClientItemEntity;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.client.world.item.ClientBlockItem;
import ritzow.sandbox.data.SerializerReaderWriter;
import ritzow.sandbox.network.WorldObjectIdentifiers;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.component.Inventory;

public class SerializationProvider {
	private static final SerializerReaderWriter provider;
			
	static {
		//TODO create SerializationReader in shared library because serialization is not needed on client side
		SerializerReaderWriter s = new SerializerReaderWriter();
		s.register(WorldObjectIdentifiers.BLOCK_GRID, BlockGrid.class, BlockGrid::new);
		s.register(WorldObjectIdentifiers.WORLD, World.class, World::new);
		s.register(WorldObjectIdentifiers.BLOCK_ITEM, ClientBlockItem.class, ClientBlockItem::new);
		s.register(WorldObjectIdentifiers.DIRT_BLOCK, ClientDirtBlock.class, ClientDirtBlock::new);
		s.register(WorldObjectIdentifiers.GRASS_BLOCK, ClientGrassBlock.class, ClientGrassBlock::new);
		s.register(WorldObjectIdentifiers.PLAYER_ENTITY, ClientPlayerEntity.class, ClientPlayerEntity::new);
		s.register(WorldObjectIdentifiers.INVENTORY, Inventory.class, Inventory::new);
		s.register(WorldObjectIdentifiers.ITEM_ENTITY, ClientItemEntity.class, ClientItemEntity::new);
		provider = s;
	}
	
	public static SerializerReaderWriter getProvider() {
		return provider;
	}
}
