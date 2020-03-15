package ritzow.sandbox.data;

public interface TransportableDataReader extends DataReader {
	<T extends Transportable> T readObject();
}
