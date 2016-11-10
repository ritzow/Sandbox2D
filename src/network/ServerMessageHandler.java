package network;

import networkutils.InvalidMessageException;
import networkutils.UnknownMessageException;

public class ServerMessageHandler extends MessageHandler {
	
	public ServerMessageHandler() {
		
	}
	
	public void handleMessage(byte[] packet) {
		try {
			constructMessage(packet);
			
		} catch (InvalidMessageException | UnknownMessageException e) {
			
		}
	}
}
