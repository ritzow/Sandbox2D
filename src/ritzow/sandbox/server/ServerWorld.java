package ritzow.sandbox.server;

import ritzow.sandbox.audio.AudioSystem;
import ritzow.sandbox.util.DataReader;
import ritzow.sandbox.world.AbstractWorld;
import ritzow.sandbox.world.entity.Entity;

public final class ServerWorld extends AbstractWorld {
	
	/** enclosing server instance **/
	private Server server;
	
	public ServerWorld(AudioSystem audio, Server server, int width, int height, float gravity) {
		super(audio, width, height, gravity);
		this.server = server;
	}
	
	public ServerWorld(DataReader reader) {
		super(reader);
	}
	
	public void setServer(Server server) {
		this.server = server;
	}
	
	@Override
	protected void onEntityAdd(Entity e) {
		server.broadcast(Server.buildSendEntity(e, server.getSerializer(), true));
	}
	
	@Override
	protected void onEntityRemove(Entity e) {
		server.broadcast(Server.buildRemoveEntity(e));
	}
	
	@Override
	protected void onEntityUpdate(Entity e) {
		server.broadcast(Server.buildGenericEntityUpdate(e));
	}
}