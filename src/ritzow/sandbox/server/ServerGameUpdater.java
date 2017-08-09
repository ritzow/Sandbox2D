package ritzow.sandbox.server;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import ritzow.sandbox.game.Lobby;
import ritzow.sandbox.server.GameTask.GameTaskType;

final class ServerGameUpdater {	
	private final Queue<GameTask> taskQueue;
	private Thread runThread;
	
	private Lobby lobby;
	private ServerWorld world;
	private boolean inLobby;
	
	ServerGameUpdater() {
		taskQueue = new ConcurrentLinkedQueue<>();
		inLobby = true;
	}
	
	private void handleTask(GameTask task) {
		switch(task.getType()) {
		case PLAYER_MOVEMENT:
			break;
		default:
		}
	}
	
	private void updateGame() {
		
	}
	
	public void submit(GameTask task) {
		taskQueue.add(task);
	}
	
	public void start() {
		if(runThread == null)
			(runThread = new Thread(this::run, "Game Update Thread")).start();
		else
			throw new RuntimeException("Game updater already running");
	}
	
	public void stop() {
		taskQueue.add(new GameTask() {
			@Override
			public GameTaskType getType() {
				return GameTaskType.TERMINATE;
			}
		});
		try {
			runThread.join();
			runThread = null;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void run() {
		//run game loop
		main_loop:
		while(true) {
			//apply all new game tasks
			while(!taskQueue.isEmpty()) {
				GameTask task = taskQueue.poll();
				if(task.getType() == GameTaskType.TERMINATE) {
					break main_loop;
				} else {
					handleTask(task);
				}
			}
			//do rest of game loop
			updateGame();
		}
		//shut down updater
	}
}
