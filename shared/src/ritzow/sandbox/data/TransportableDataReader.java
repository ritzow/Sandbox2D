package ritzow.sandbox.data;

public interface TransportableDataReader extends DataReader {
	public <T extends Transportable> T readObject();
}
