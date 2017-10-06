package ritzow.sandbox.client.graphics;

import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.glDrawArrays;

import ritzow.sandbox.world.component.Luminous;

public final class LightRenderProgram extends ShaderProgram {
	
	private final int
		uniform_lightColor,
		uniform_lightRadius,
		uniform_lightIntensity,
		uniform_lightPosX,
		uniform_lightPosY;
	
	private final Camera camera;
	
	public LightRenderProgram(Shader vertex, Shader fragment, Camera camera) {
		super(vertex, fragment);
		uniform_lightColor = getUniformID("lightColor");
		uniform_lightRadius = getUniformID("lightRadius");
		uniform_lightIntensity = getUniformID("lightIntensity");
		uniform_lightPosX = getUniformID("lightPosX");
		uniform_lightPosY = getUniformID("lightPosY");
		this.camera = camera;
	}
	
	public void render(Luminous light, float posX, float posY, int framebufferWidth, int framebufferHeight) {
		float red = light.getLightRed();
		float green = light.getLightGreen();
		float blue = light.getLightBlue();
		
		if(red > 1 || red < 0 || green > 1 || green < 0 || blue > 1 || blue < 0) {
			throw new RuntimeException("invalid color value(s)");
		}

		//draw a quad to the screen (the vertices are inside the fragment shader)
		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		//load the color, light radius, light intensity, and light position uniforms
		setVector(uniform_lightColor, red, green, blue);
		setFloat(uniform_lightRadius, light.getLightRadius() * camera.getZoom()); //does not scale properly, perhaps shader needs to be modified
		setFloat(uniform_lightIntensity, light.getLightIntensity());
		float pixelOriginX = normalizedToScreen((posX - camera.getPositionX()) * camera.getZoom() * framebufferHeight/framebufferWidth, framebufferWidth);
		float pixelOriginY = normalizedToScreen((posY - camera.getPositionY()) * camera.getZoom(), framebufferHeight);
		setFloat(uniform_lightPosX, pixelOriginX);
		setFloat(uniform_lightPosY, pixelOriginY);
	}
	
	private static final float normalizedToScreen(float normal, float screenDimension) {
		return screenDimension * (normal + 1)/2;
	}
}
