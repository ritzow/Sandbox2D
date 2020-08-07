package ritzow.sandbox.client.graphics;

public class StaticLight extends Light {
	private final float red, green, blue;
	private final float posX, posY;
	private final float intensity; //TODO this is basically the radius

	public StaticLight(float posX, float posY, float r, float g, float b, float intensity) {
		this.posX = posX;
		this.posY = posY;
		this.red = r;
		this.green = g;
		this.blue = b;
		this.intensity = intensity;
	}

	@Override
	public float posX() {
		return posX;
	}

	@Override
	public float posY() {
		return posY;
	}

	@Override
	public float red() {
		return red;
	}

	@Override
	public float green() {
		return green;
	}

	@Override
	public float blue() {
		return blue;
	}

	@Override
	public float intensity() {
		return intensity;
	}
}
