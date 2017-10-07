package ritzow.sandbox.server;

import ritzow.sandbox.protocol.WorldObjectIdentifiers;
import ritzow.sandbox.server.world.ServerWorld;
import ritzow.sandbox.server.world.block.DirtBlock;
import ritzow.sandbox.server.world.block.GrassBlock;
import ritzow.sandbox.server.world.entity.ItemEntity;
import ritzow.sandbox.server.world.entity.ParticleEntity;
import ritzow.sandbox.server.world.entity.PlayerEntity;
import ritzow.sandbox.server.world.item.BlockItem;
import ritzow.sandbox.util.SerializerReaderWriter;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.component.Inventory;

public class SerializationProvider {
	private static final SerializerReaderWriter provider;
	
	static {
		provider = new SerializerReaderWriter();
		provider.register(WorldObjectIdentifiers.BLOCK_GRID, BlockGrid.class, BlockGrid::new);
		provider.register(WorldObjectIdentifiers.WORLD, ServerWorld.class, ServerWorld::new);
		provider.register(WorldObjectIdentifiers.BLOCK_ITEM, BlockItem.class, BlockItem::new);
		provider.register(WorldObjectIdentifiers.DIRT_BLOCK, DirtBlock.class, DirtBlock::new);
		provider.register(WorldObjectIdentifiers.GRASS_BLOCK, GrassBlock.class, GrassBlock::new);
		provider.register(WorldObjectIdentifiers.PLAYER_ENTITY, PlayerEntity.class, PlayerEntity::new);
		provider.register(WorldObjectIdentifiers.INVENTORY, Inventory.class, Inventory::new);
		provider.register(WorldObjectIdentifiers.ITEM_ENTITY, ItemEntity.class, ItemEntity::new);
		provider.register(WorldObjectIdentifiers.PARTICLE_ENTITY, ParticleEntity.class, ParticleEntity::new);
	}
	
	public static SerializerReaderWriter getProvider() {
		return provider;
	}
}
