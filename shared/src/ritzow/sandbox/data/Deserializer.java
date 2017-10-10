package ritzow.sandbox.data;

public interface Deserializer {
	public <T> T deserialize(byte[] object);
}
