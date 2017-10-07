package ritzow.sandbox.server;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import ritzow.sandbox.server.world.ServerWorld;

interface GameTask {
	public void execute(ServerGameUpdater program);
}

final class ServerGameUpdater {	
	private final Queue<GameTask> taskQueue;
	Thread runThread;
	
	ServerGameUpdater() {
		taskQueue = new ConcurrentLinkedQueue<>();
	}
	
	//important game stuff
	static final long MAX_TIMESTEP = 2;
	
	ServerWorld world;
	long previousTime;
	
	private void updateGame() {
		if(world != null) {
			long current = System.nanoTime(); //get the current time
			float totalUpdateTime = (current - previousTime) * 0.0000000625f; //get the amount of update time
			previousTime = current; //update the previous time for the next frame
			
			//update the world with a timestep of at most 2 until the world is up to date.
			for(float time; totalUpdateTime > 0; totalUpdateTime -= time) {
				time = Math.min(totalUpdateTime, MAX_TIMESTEP);
				world.update(time);
				totalUpdateTime -= time;
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
	
	//control flow handling section
	
	private boolean exit;
	
	public void submit(GameTask task) {
		if(runThread == null)
			throw new RuntimeException("Game updater has not started");
		taskQueue.add(Objects.requireNonNull(task));
	}
	
	public void start() {
		if(runThread == null)
			(runThread = new Thread(this::run, "Game Update Thread")).start();
		else
			throw new RuntimeException("Game updater already running");
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

	private void run() {
		//run game loop
		while(!exit) {
			//apply all new game tasks
			while(!taskQueue.isEmpty()) {
				taskQueue.poll().execute(this); //allow the task to execute itself using this updater instance
				//if a task set exit to true, stop updating
				if(exit)
					break;
			}
			
			if(exit) //TODO bad convention, rewrite entire loop somehow!?
				break;
			
			//do rest of game loop
			updateGame();
		}
		//shut down updater
		cleanup();
	}
}
