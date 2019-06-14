package ritzow.sandbox.client;

public interface GameContext {
	void run();
	default String name() {
		return "Unnamed";
	}
}