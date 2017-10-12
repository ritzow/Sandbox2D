package ritzow.sandbox.server;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import ritzow.sandbox.world.World;

interface GameTask {
	public void execute(ServerGameUpdater program);
}

//TODO integrate this with Server, take packets directly from network controller instead of from thread pool from networkcontroller
final class ServerGameUpdater {	
	private final Queue<GameTask> taskQueue;
	private boolean exit;
	
	static final long MAX_TIMESTEP = 2;
	
	Server server;
	Thread runThread;
	World world;
	long previousTime;
	
	public ServerGameUpdater(Server server) {
		taskQueue = new ConcurrentLinkedQueue<>();
		this.server = server;
	}
	
	private void updateGame() {
		if(world != null) {
			long current = System.nanoTime(); //get the current time
			float totalUpdateTime = (current - previousTime) * 0.0000000625f; //get the amount of update time
			previousTime = current; //update the previous time for the next frame

			//update the world with a timestep of at most MAX_TIMESTEP until the world is up to date.
			for(float time; totalUpdateTime > 0 && !exit; totalUpdateTime -= time) {
				time = Math.min(totalUpdateTime, MAX_TIMESTEP);
				world.update(time);
				totalUpdateTime -= time;
			}
			
			world.forEach(e -> {
				server.sendEntityUpdate(e);
			});
			
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
	
//	public void pause() {
//		throw new UnsupportedOperationException("not implemented");
//	}
//	
//	public void isPaused() {
//		throw new UnsupportedOperationException("not implemented");
//	}
//	
//	public void resume() {
//		throw new UnsupportedOperationException("not implemented");
//	}
	
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
