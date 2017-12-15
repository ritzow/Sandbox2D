package ritzow.sandbox.server;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.Entity;

@FunctionalInterface
interface GameTask {
	public void execute(ServerGameUpdater program);
}

final class ServerGameUpdater {	
	private final Queue<GameTask> taskQueue;
	private boolean exit;
	
	static final long MAX_TIMESTEP = 2;
	static final long NETWORK_SEND_INTERVAL_NANOSECONDS = 50_000_000;
	
	final Server server;
	Thread runThread;
	World world;
	long previousTime, lastUpdateSend;
	
	public ServerGameUpdater(Server server) {
		taskQueue = new ConcurrentLinkedQueue<>();
		this.server = server;
	}
	
	private void updateGame() {
		if(world != null) {
			long current = System.nanoTime(); //get the current time
			float totalUpdateTime = (current - previousTime) / 16_000_000f;
			previousTime = current; //update the previous time for the next frame

			//update the world with a timestep of at most MAX_TIMESTEP until the world is up to date.
			for(float time = totalUpdateTime; totalUpdateTime > 0 && !exit; totalUpdateTime -= time) {
				time = Math.min(totalUpdateTime, MAX_TIMESTEP);
				world.update(time);
				totalUpdateTime -= time;
			}
			
			if(System.nanoTime() - lastUpdateSend > NETWORK_SEND_INTERVAL_NANOSECONDS) {
				for(Entity e : world) {
					server.sendEntityUpdate(e);
				}
				lastUpdateSend = System.nanoTime();
			}
			
			try {
				Thread.sleep(1); //sleep so cpu isnt wasted
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void cleanup() {
		world = null;
	}
	
	public void submit(GameTask task) {
		if(runThread == null)
			throw new RuntimeException("Game updater has not started");
		taskQueue.add(Objects.requireNonNull(task));
	}
	
	private void run() {
		while(!exit) {
			if(!taskQueue.isEmpty()) {
				taskQueue.poll().execute(this);
			} else {
				updateGame();
			}
		}
		cleanup();
	}
	
	public boolean isRunning() {
		return runThread != null && runThread.isAlive() && !exit;
	}
	
	public void start() {
		if(runThread == null) {
			exit = false;
			previousTime = System.nanoTime();
			(runThread = new Thread(this::run, "Game Update Thread")).start();
		} else {
			throw new RuntimeException("Game updater already running");
		}
	}
	
	public void stop() {
		try {
			submit(u -> {
				u.exit = true;
			});
			runThread.join();
			runThread = null;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
