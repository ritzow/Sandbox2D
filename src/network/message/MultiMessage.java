package network.message;

import util.ByteUtil;

/**
 * @author Solomon Ritzow
 *
 */
public class MultiMessage extends Message {
	
	protected Message[] messages;
	
	public MultiMessage(Message[] messages) {
		this.messages = messages;
	}

	@Override
	public byte[] getBytes() {
		byte[][] messageBytes = new byte[messages.length][];
		int totalBytes = 0;
		for(int i = 0; i < messages.length; i++) {
			messageBytes[i] = messages[i].getBytes();
			totalBytes += messageBytes[i].length;
		}
		
		byte[] flattened = new byte[2 + totalBytes];
		int index = 0;
		for(int i = 0; i < messageBytes.length; i++) {
			System.arraycopy(messageBytes[i], 0, flattened, index, messageBytes[i].length);
			index += messageBytes[i].length;
		}
		ByteUtil.putShort(flattened, 0, Protocol.MULTI_MESSAGE);
		//TODO add messages' lengths directly before each message
		return flattened;
	}

	@Override
	public String toString() {
		return "multi message";
	}

}
