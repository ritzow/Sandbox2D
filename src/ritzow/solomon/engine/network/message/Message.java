package ritzow.solomon.engine.network.message;

public interface Message {
	public abstract byte[] getBytes();
	public abstract boolean isReliable();
}
