package ritzow.solomon.engine.network.message;

import ritzow.solomon.engine.util.ByteUtil;

public class RequestDenial implements Message {
	
	protected final String reason;
	
	public RequestDenial(byte[] request) {
		int length = ByteUtil.getInteger(request, 0);
		reason = new String(request, 4, length);
	}
	
	public RequestDenial(String reason) {
		this.reason = reason;
	}

	@Override
	public byte[] getBytes() {
		byte[] reasonString = reason.getBytes();
		byte[] message = new byte[4 + reasonString.length];
		ByteUtil.putInteger(message, 0, reasonString.length);
		ByteUtil.write(reasonString, message, 4);
		return message;
	}

	@Override
	public boolean isReliable() {
		return true;
	}

}
