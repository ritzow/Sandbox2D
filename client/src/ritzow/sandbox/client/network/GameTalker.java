package ritzow.sandbox.client.network;

public interface GameTalker {
	void sendBlockBreak(int x, int y);
	void sendBlockPlace(int x, int y);
	void sendBombThrow(float angle);
}
