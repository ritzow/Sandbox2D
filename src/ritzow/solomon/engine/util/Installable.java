package ritzow.solomon.engine.util;

/**
 * This interface should be implemented by objects which have a startup period before functioning normally.
 * @author Solomon Ritzow
 *
 */
public interface Installable {
	public boolean isSetupComplete();
	
	default public void waitForSetup() {
		synchronized(this) {
			while(!isSetupComplete()) {
				try {
					wait();
				} catch (InterruptedException e){
					continue;
				}
			}
		}
	}
}
