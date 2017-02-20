package ritzow.solomon.engine.util;

public interface Service extends Installable, Exitable, Runnable {
	default boolean isRunning() {
		return isSetupComplete() && !isFinished();
	}
}
