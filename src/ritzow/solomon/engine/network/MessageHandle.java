package ritzow.solomon.engine.network;

public final class MessageHandle {
	protected final int messageID;
	protected boolean received;
	
	public MessageHandle(int messageID) {
		this.messageID = messageID;
	}
	
	public final int getMessageID() {
		return messageID;
	}
	
	public synchronized boolean waitForResponse() throws InterruptedException {
		return waitForResponse(0);
	}
	
	public synchronized boolean waitForResponse(long milliseconds) throws InterruptedException {
		this.wait(milliseconds);
		return received;
	}
	
	public final synchronized void notifyReceived() {
		received = true;
		this.notifyAll();
	}
}
