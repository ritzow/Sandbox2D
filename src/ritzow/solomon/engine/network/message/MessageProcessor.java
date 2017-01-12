package ritzow.solomon.engine.network.message;

import java.net.SocketAddress;

/**
 * Provides a way to process different message types on an opt-in basis
 * @author Solomon Ritzow
 */
public interface MessageProcessor {
	void processMessage(int messageID, short protocol, SocketAddress sender, byte[] data);
}
