package ritzow.sandbox.data;

public interface Serializer {
	/**
	 * Serializes a Transportable object
	 * @param object a non-null Transportable
	 * @return a byte array containing the serialized object (never returns null), which can be read by a Deserializer
	 */
	byte[] serialize(Transportable object);
}
