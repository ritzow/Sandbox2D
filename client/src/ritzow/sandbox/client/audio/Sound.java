package ritzow.sandbox.client.audio;

public enum Sound {
	BLOCK_BREAK(0),
	BLOCK_PLACE(1),
	POP(2),
	SNAP(3),
	THROW(4);
	
	private int code;
	
	Sound(int code) {
		this.code = code;
	}
	
	public int code() {
		return code;
	}
}
