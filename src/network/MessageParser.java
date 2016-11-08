package network;

import static networkutils.ByteUtil.getShort;

import network.message.*;
import networkutils.InvalidMessageException;
import networkutils.Message;
import networkutils.UnknownMessageException;

public class MessageParser {
	public static Message getMessage(byte[] packet) throws UnknownMessageException, InvalidMessageException {
		
		if(packet.length < 2) {
			throw new InvalidMessageException();
		}
		
		else {
			short protocol = getShort(packet, 0);
			switch(protocol) {
			case 1:
				return new ServerConnectRequest();
			case 2:
				return new ClientInfoMessage(packet);
			case 3:
				return new ServerInfoMessage(packet);
			case 5:
				return new ServerInfoRequest();
			default:
				throw new UnknownMessageException();
			}
		}
	}
}
