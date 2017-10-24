package ritzow.sandbox.client.graphics;

public class TextureImage {
	private final int width;
	private final int bytesPerPixel;
	private final byte[] data;
	
	public TextureImage(int width, int bytesPerPixel, byte[] data) {
		this.width = width;
		this.bytesPerPixel = bytesPerPixel;
		this.data = data;
	}
	
	public int getWidth() {
		return width;
	}
	
	public byte[] getData() {
		return data;
	}

	public int getBytesPerPixel() {
		return bytesPerPixel;
	}
}
