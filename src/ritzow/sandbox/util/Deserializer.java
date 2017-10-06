package ritzow.sandbox.util;

public interface Deserializer {
	public <T> T deserialize(byte[] object);
}
