package ritzow.sandbox.server;

import ritzow.sandbox.util.RepeatUpdater;
import ritzow.sandbox.util.SharedConstants;
import ritzow.sandbox.util.TaskQueue;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;

public final class ServerRepeatUpdater extends RepeatUpdater {
	private volatile World world;
	private final Server server;
	private final TaskQueue tasks;
	private long previousTime, lastSendTime;
	
	private final Runnable worldUpdater = this::updateWorld;
	
	private static final long NETWORK_SEND_INTERVAL_NANOSECONDS = 50_000_000;
	
	public ServerRepeatUpdater(Server server) {
		this.server = server;
		addRepeatTask(tasks = new TaskQueue());
	}
	
	public World getWorld() {
		return world;
	}
	
	public void startWorld(World world) {
		if(this.world == null)
			addRepeatTask(worldUpdater);
		this.world = world;
		previousTime = System.nanoTime();
	}
	
	private void updateWorld() {
		previousTime = Utility.updateWorld(world, previousTime, SharedConstants.MAX_TIMESTEP, SharedConstants.TIME_SCALE_NANOSECONDS);
		if(System.nanoTime() - lastSendTime > NETWORK_SEND_INTERVAL_NANOSECONDS) {
			for(Entity e : world)
				server.sendEntityUpdate(e);
			lastSendTime = System.nanoTime();
		}
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void stopWorld() {
		removeRepeatTask(worldUpdater);
	}
	
	public void submitTask(Runnable task) {
		tasks.add(task);
	}
}
