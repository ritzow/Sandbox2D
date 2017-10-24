package ritzow.sandbox.client.util;

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

public class SerializationProvider {
	private static final SerializerReaderWriter provider;
			
	static {
		//TODO create SerializationReader in shared library because serialization is not needed on client side
		SerializerReaderWriter s = new SerializerReaderWriter();
		s.register(Protocol.BLOCK_GRID, BlockGrid.class, BlockGrid::new);
		s.register(Protocol.WORLD, World.class, World::new);
		s.register(Protocol.BLOCK_ITEM, ClientBlockItem.class, ClientBlockItem::new);
		s.register(Protocol.DIRT_BLOCK, ClientDirtBlock.class, ClientDirtBlock::new);
		s.register(Protocol.GRASS_BLOCK, ClientGrassBlock.class, ClientGrassBlock::new);
		s.register(Protocol.PLAYER_ENTITY, ClientPlayerEntity.class, ClientPlayerEntity::new);
		s.register(Protocol.INVENTORY, Inventory.class, Inventory::new);
		s.register(Protocol.ITEM_ENTITY, ClientItemEntity.class, ClientItemEntity::new);
		provider = s;
	}
	
	public static SerializerReaderWriter getProvider() {
		return provider;
	}
}
