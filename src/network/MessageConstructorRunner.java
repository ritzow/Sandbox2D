package network;

import networkutils.InvalidMessageException;
import networkutils.UnknownMessageException;
import static networkutils.ByteUtil.*;
import network.message.*;

public class MessageConstructorRunner {
	
	protected void constructMessage(byte[] packet, MessageHandler handler) throws UnknownMessageException, InvalidMessageException {
		if(packet.length < 2)
			throw new InvalidMessageException();
		
		short protocol = getShort(packet, 0);
		
		switch(protocol) {
		case 1:
			handler.handleMessage(new ServerConnectRequest());
			return;
		case 2:
			handler.handleMessage(new ClientInfoMessage(packet));
			return;
		case 3:
			handler.handleMessage(new ServerInfoMessage(packet));
			return;
		case 5:
			handler.handleMessage(new ServerInfoRequest());
			return;
		case 50:
			handler.handleMessage(new ServerConnectRequest());
			return;
		default:
			throw new UnknownMessageException();
		}
	}
}
