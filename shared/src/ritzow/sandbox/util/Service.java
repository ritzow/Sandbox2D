package ritzow.sandbox.util;

public interface Service extends Installable, Runnable, Exitable {
	default boolean isRunning() {
		return isSetupComplete() && !isFinished();
	}
	
	default void start() {
		new Thread(this, this.getClass().getSimpleName()).start();
	}
}
