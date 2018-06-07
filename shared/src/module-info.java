/**
 * Code shared between Sandbox2D client and server, contains Entity implementations, World, BlockGrid, and the
 * serialization system in ritzow.sandbox.data. 
 * @author Solomon Ritzow
 */
module ritzow.sandbox.shared {
	exports ritzow.sandbox.network;
	exports ritzow.sandbox.util to
		ritzow.sandbox.client,
		ritzow.sandbox.server;
	exports ritzow.sandbox.world;
	exports ritzow.sandbox.world.component;
	exports ritzow.sandbox.world.item;
	exports ritzow.sandbox.world.entity;
	exports ritzow.sandbox.world.block;
	exports ritzow.sandbox.data;
}