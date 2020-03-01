package ritzow.sandbox.data;

import java.util.function.Function;

public interface Deserializer {
	public <T> T deserialize(byte[] object);

	interface ObjectBuilder extends Function<TransportableDataReader, Transportable> {}
}
