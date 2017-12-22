package ritzow.sandbox.client.graphics;

class TextureImage implements Comparable<TextureImage> {
	private final int width;
	private final int bytesPerPixel;
	private final byte[] data;
	
	/**
	 * Stores information about a texture.
	 * @param widthPixels the texture's width, in pixels
	 * @param bytesPerPixel the number of bytes per pixel
	 * @param data the texture data
	 */
	public TextureImage(int widthPixels, int bytesPerPixel, byte[] data) {
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

	@Override
	public int compareTo(TextureImage other) { //larger (more pixels) textures come first
		return other.getWidth() * other.getHeight() - getWidth() * getHeight();
	}
}
