package ritzow.solomon.engine.network.message;

public class MessageHandle {
	protected int messageID;
	protected boolean received;
	protected byte[] packet;
	
	public MessageHandle(int messageID) {
		this.messageID = messageID;
	}
	
	public final int getMessageID() {
		return messageID;
	}
	
	public final void notifyReceived() {
		received = true;
		this.notifyAll();
	}
}
