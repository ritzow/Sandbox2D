package ritzow.solomon.engine.network;

import java.nio.charset.Charset;
import ritzow.solomon.engine.util.ByteUtil;

public final class ConsoleMessage {
	
	private final byte[] message;
	
	public ConsoleMessage(String message) {
		this.message = message.getBytes(Charset.forName("UTF-8"));
	}
	
	public byte[] getBytes() {
		byte[] packet = new byte[2 + message.length];
		ByteUtil.putShort(packet, 0, Protocol.CONSOLE_MESSAGE);
		ByteUtil.copy(message, packet, 2);
		return packet;
	}
}
