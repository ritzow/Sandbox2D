package ritzow.sandbox.util;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TaskQueue extends ConcurrentLinkedQueue<Runnable> implements Runnable {

	@Override
	public void run() {
		while(!isEmpty())
			remove().run();
	}
}
