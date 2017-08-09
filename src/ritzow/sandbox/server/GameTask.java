package ritzow.sandbox.server;

public interface GameTask {
	public enum GameTaskType {
		PLAYER_MOVEMENT,
		TERMINATE
	}
	
	public GameTaskType getType();
}
