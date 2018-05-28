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
	
	private static final long NETWORK_SEND_INTERVAL_NANOSECONDS = Utility.millisToNanos(5);
	
	public ServerRepeatUpdater(Server server) {
		this.server = server;
		repeatTasks().add(tasks = new TaskQueue());
	}
	
	public World getWorld() {
		return world;
	}
	
	public void startWorld(World world) {
		if(this.world == null)
			repeatTasks().add(worldUpdater);
		this.world = world;
		previousTime = System.nanoTime();
	}
	
	private final Runnable worldUpdater = this::updateWorld;
	
	private void updateWorld() {
		previousTime = Utility.updateWorld(world, previousTime, SharedConstants.MAX_TIMESTEP, SharedConstants.TIME_SCALE_NANOSECONDS);
		if(Utility.nanosSince(lastSendTime) > NETWORK_SEND_INTERVAL_NANOSECONDS) {
			for(Entity e : world)
				server.sendUpdateEntity(e);
			server.broadcastPing(); //send a reliable packet to make sure clients are connected
			lastSendTime = System.nanoTime();
		}
		
		Utility.sleep(1);
	}
	
	public void stopWorld() {
		repeatTasks().remove(worldUpdater);
	}
	
	public void submitTask(Runnable task) {
		tasks.add(task);
	}
}
