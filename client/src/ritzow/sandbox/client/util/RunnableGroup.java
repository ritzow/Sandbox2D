package ritzow.sandbox.client.util;

import java.util.Collection;

public class RunnableGroup implements Runnable {
	private final Collection<Runnable> runnables;

	public RunnableGroup(Collection<Runnable> runnables) {
		this.runnables = runnables;
	}
	
	@Override
	public void run() {
		for(Runnable r : runnables) {
			r.run();
		}
	}
}
