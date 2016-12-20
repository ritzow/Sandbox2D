package ritzow.solomon.engine.network.message;

public class DataTransferMessage implements Message {
	
	protected byte[] data;
	
	public DataTransferMessage(byte[] data) {
		this.data = data;
	}

	@Override
	public byte[] getBytes() {
		return data;
	}

	@Override
	public boolean isReliable() {
		return true;
	}
}
