package ritzow.sandbox.server;

public interface Message {
	public byte[] toBytes();
	public boolean isReliable();
}
