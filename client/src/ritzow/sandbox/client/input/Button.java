package ritzow.sandbox.client.input;

public record Button(byte type, int code) implements Control {
	public boolean equals(byte type, int code) {
		return this.type == type && this.code == code;
	}
}