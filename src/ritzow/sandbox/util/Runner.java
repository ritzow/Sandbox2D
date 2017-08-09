package ritzow.sandbox.util;

public abstract class Runner {
	private final Object lock = new Object();
	private volatile boolean started, stopped;
	
	protected abstract void onStart();
	protected abstract void onStop();
	
	public final void start() {
		onStart();
		synchronized(lock) {
			started = true;
			lock.notifyAll();
		}
	}
	
	public final void stop() {
		onStop();
		synchronized(lock) {
			stopped = true;
			lock.notifyAll();
		}
	}
	
	public final void waitUntilStarted() {
		synchronized(lock) {
			while(!started) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public final void waitUntilStopped() {
		synchronized(lock) {
			while(!stopped) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
