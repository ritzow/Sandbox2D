package ritzow.sandbox.util;

public interface Service extends Runnable, Exitable {
	public boolean isSetupComplete();
	
	public default void waitForSetup() {
		Utility.waitOnCondition(this, this::isSetupComplete);
	}
	
	public default boolean isRunning() {
		return isSetupComplete() && !isFinished();
	}
	
	public default void start() {
		new Thread(this, this.getClass().getSimpleName()).start();
	}
}
