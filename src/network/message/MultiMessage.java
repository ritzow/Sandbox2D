package network.message;

/**
 * @author Solomon Ritzow
 *
 */
public class MultiMessage implements Message {
	
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
		
		byte[] flattened = new byte[totalBytes];
		int index = 0;
		for(int i = 0; i < messageBytes.length; i++) {
			System.arraycopy(messageBytes[i], 0, flattened, index, messageBytes[i].length);
			index += messageBytes[i].length;
		}
		return flattened;
	}

	@Override
	public String toString() {
		return "multi message";
	}

}
