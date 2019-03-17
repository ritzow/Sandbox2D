package ritzow.sandbox.client.network;

import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.network.Protocol.PlayerAction;

public interface GameTalker {
	void sendPlayerAction(PlayerAction action);
	ClientPlayerEntity getPlayer() throws InterruptedException;
	void sendBlockBreak(int x, int y);
	void sendBombThrow(float angle);
}
