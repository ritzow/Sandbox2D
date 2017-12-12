package ritzow.sandbox.client.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import ritzow.sandbox.util.Utility;

/**
 * An intance of ClientGameUpdater manages a 'game loop', which repeatedly runs a list of
 * tasks that need to be executed each game update.
 * @author Solomon Ritzow
 */
public final class ClientGameUpdater {
	private Thread thread;
	private final Object resumeLock;
	private volatile boolean paused, exit;
	private final List<Runnable> runnables;
	private Runnable pre, post;
	
	public ClientGameUpdater(Runnable onStart, Runnable onExit) {
		this.runnables = Collections.synchronizedList(new LinkedList<Runnable>());
		this.paused = true;
		this.pre = onStart;
		this.post = onExit;
		resumeLock = new Object();
	}
	
	public void start() {
		if(thread != null)
			throw new IllegalStateException("updater already started");
		(thread = new Thread(this::run, "Game Updater")).start();
	}
	
	public void stop() {
		if(thread == null)
			throw new IllegalStateException("updater hasn't been started");
		try {
			exit = true;
			thread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException("client updater shutdown should not be interrupted", e);
		}
	}
	
	public void pause() {
		paused = true;
	}
	
	public void resume() {
		if(paused) {
			paused = false;
			synchronized(resumeLock) {
				resumeLock.notifyAll();
			}
		}
	}
	
	public void waitForSetup() {
		Utility.waitOnCondition(resumeLock, () -> !paused);
	}
	
	public void addRepeatedTaskBefore(Runnable task, Runnable taskAfter) {
		runnables.add(runnables.indexOf(taskAfter), task);
	}
	
	public void addRepeatedTask(Runnable task) {
		runnables.add(task);
	}
	
	public void addRepeatedTasks(Collection<? extends Runnable> tasks) {
		runnables.addAll(tasks);
	}
	
	public void removeRepeatedTask(Runnable task) {
		runnables.remove(task);
	}
	
	public void removeRepeatedTasks(Collection<Runnable> tasks) {
		runnables.removeAll(tasks);
	}
	
	private void run() {
		pre.run();
		paused = false;
		synchronized(resumeLock) {
			resumeLock.notifyAll();
		}
		
		while(!exit) {
			synchronized(runnables) {
				for(Runnable r : runnables) {
					r.run();
				}
			}
			Utility.waitOnCondition(resumeLock, () -> !paused);
		}
		
		post.run();
	}
}
