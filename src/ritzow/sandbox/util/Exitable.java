package ritzow.sandbox.util;

/**
 * Instances of Exitable can be passed to the Synchronizer utility to wait until exited. When an Exitable finishes, the isFinished method should return true and
 * the object should call notifyAll()
 * @author Solomon Ritzow
 *
 */
public interface Exitable {
	public void exit();
	public boolean isFinished();
	
	public default void waitUntilFinished() {
		synchronized(this) {
			while(!isFinished()) {
				try {
					wait();
				} catch (InterruptedException e) {
					continue;
				}
			}
		}
	}
	
	public default void waitForExit() {
		exit();
		waitUntilFinished();
	}
}
