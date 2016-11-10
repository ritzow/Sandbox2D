package network;

import static networkutils.ByteUtil.getShort;

import network.message.ClientInfoMessage;
import network.message.ServerConnectRequest;
import network.message.ServerInfoMessage;
import network.message.ServerInfoRequest;
import networkutils.InvalidMessageException;
import networkutils.Message;
import networkutils.UnknownMessageException;

public class MessageHandler {
	protected Message constructMessage(byte[] packet) throws InvalidMessageException, UnknownMessageException {
		if(packet.length < 2) {
			throw new InvalidMessageException();
		}
		
		else {//TODO create a "Rule Engine" instead of this big messy switch statement?
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
