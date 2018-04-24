package ritzow.sandbox.client.graphics;

public class TextureData implements Comparable<TextureData> {
	private final int width;
	private final int bytesPerPixel;
	private final byte[] data;
	
	/**
	 * Stores information about a texture.
	 * @param widthPixels the texture's width, in pixels
	 * @param bytesPerPixel the number of bytes per pixel
	 * @param data the texture data
	 */
	public TextureData(int widthPixels, int bytesPerPixel, byte[] data) {
		this.width = widthPixels;
		this.bytesPerPixel = bytesPerPixel;
		this.data = data;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return data.length / (bytesPerPixel * width); //total size / bytes per row
	}
	
	public byte[] getData() {
		return data;
	}

	public int getBytesPerPixel() {
		return bytesPerPixel;
	}
	
	private int pixelCount() {
		return getWidth() * getHeight();
	}

	@Override
	public int compareTo(TextureData other) {
		return other.pixelCount() - pixelCount(); //images with more pixels come first
	}

	@Override
	public String toString() {
		return "TextureData [width=" + width + ", bytesPerPixel=" + bytesPerPixel + ", data=" + data.length + " bytes]";
	}
}
