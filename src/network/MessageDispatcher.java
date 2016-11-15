package network;

import networkutils.InvalidMessageException;
import networkutils.UnknownMessageException;
import static networkutils.ByteUtil.*;
import network.message.*;

public class MessageDispatcher {
	
	public void process(byte[] packet, MessageHandler handler) throws UnknownMessageException, InvalidMessageException {
		if(packet.length < 2)
			throw new InvalidMessageException();
		
		short protocol = getShort(packet, 0);
		
		switch(protocol) {
		case Protocol.SERVER_CONNECT_REQUEST:
			handler.handleMessage(new ServerConnectRequest());
			return;
		case Protocol.CLIENT_INFO:
			handler.handleMessage(new ClientInfoMessage(packet));
			return;
		case Protocol.SERVER_INFO:
			handler.handleMessage(new ServerInfoMessage(packet));
			return;
		case Protocol.SERVER_INFO_REQUEST:
			handler.handleMessage(new ServerInfoRequest());
			return;
		default:
			throw new UnknownMessageException();
		}
	}
}
