package ritzow.sandbox.client.ui;

public interface Animation {
	void update(long progressNanos, float completionRatio, long nanosSinceLast);
	default void onEnd() {}
}
