package ritzow.solomon.engine.world.base;

import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.glDrawArrays;

import ritzow.solomon.engine.graphics.GraphicsUtility;
import ritzow.solomon.engine.graphics.Shader;
import ritzow.solomon.engine.graphics.ShaderProgram;
import ritzow.solomon.engine.world.component.Luminous;

public final class LightRenderProgram extends ShaderProgram {
	
	private final int 
		uniform_lightColor,
		uniform_lightRadius,
		uniform_lightIntensity,
		uniform_lightPosX,
		uniform_lightPosY;
	
	public LightRenderProgram(Shader vertex, Shader fragment) {
		super(vertex, fragment);
		GraphicsUtility.printShaderCompilation(vertex, fragment);
		
		uniform_lightColor = getUniformID("lightColor");
		uniform_lightRadius = getUniformID("lightRadius");
		uniform_lightIntensity = getUniformID("lightIntensity");
		uniform_lightPosX = getUniformID("lightPosX");
		uniform_lightPosY = getUniformID("lightPosY");
		
		System.out.println("light color uniform: " + uniform_lightRadius);
	}
	
	public void render(Luminous light, float posX, float posY) {
		glDrawArrays(GL_POINTS, 0, 0);
		setVector(uniform_lightColor, light.getLightRed(), light.getLightGreen(), light.getLightBlue());
		setFloat(uniform_lightRadius, light.getLightRadius());
		setFloat(uniform_lightIntensity, light.getLightIntensity());
		setFloat(uniform_lightPosX, posX);
		setFloat(uniform_lightPosY, posY); //TODO these may need to be transformed from world coordinates
	}
}
