package ritzow.sandbox.util;

import java.util.Collection;
import java.util.Collections;
//import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * RepeatRunnable will repeatedly run any Runnables provided via addRepeatRunnable(s) in the order they were added
 * once start is called, until stop is called.
 * @author Solomon Ritzow
 */
public class RepeatUpdater {
	private Thread thread;
	private final Object resumeLock;
	private volatile boolean paused, exit;
	private final List<Runnable> runnables;
	private final Runnable pre, post;
	
	public RepeatUpdater(Runnable onStart, Runnable onExit) {
		this.runnables = Collections.synchronizedList(new LinkedList<Runnable>());
		this.paused = true;
		this.pre = Objects.requireNonNull(onStart);
		this.post = Objects.requireNonNull(onExit);
		resumeLock = new Object();
	}
	
	public RepeatUpdater() {
		this(() -> {}, () -> {});
	}
	
	public void start() {
		if(thread != null)
			throw new IllegalStateException("updater already started");
		(thread = new Thread(this::run, "repeat updater")).start();
	}
	
	public void stop() {
		if(thread == null)
			throw new IllegalStateException("updater hasn't been started");
		try {
			exit = true;
			thread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException("repeat updater shutdown was interrupted", e);
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
	
	public RepeatUpdater addRepeatTaskBefore(Runnable task, Runnable taskAfter) {
		runnables.add(runnables.indexOf(taskAfter), task);
		return this;
	}
	
	public RepeatUpdater addRepeatTaskAfter(Runnable task, Runnable taskBefore) {
		runnables.add(runnables.indexOf(taskBefore) + 1, task);
		return this;
	}
	
	public RepeatUpdater addRepeatTask(Runnable task) {
		runnables.add(task);
		return this;
	}
	
	public RepeatUpdater addRepeatTasks(Collection<? extends Runnable> tasks) {
		runnables.addAll(tasks);
		return this;
	}
	
	public void removeRepeatTask(Runnable task) {
		runnables.remove(task);
	}
	
	public void removeRepeatTasks(Collection<Runnable> tasks) {
		runnables.removeAll(tasks);
	}
	
	private void run() {
		pre.run();
		paused = false;
		synchronized(resumeLock) {
			resumeLock.notifyAll();
		}
		
		while(!exit) {
			runnables.forEach(r -> r.run());
			Utility.waitOnCondition(resumeLock, () -> !paused);
		}
		
		post.run();
	}
}
