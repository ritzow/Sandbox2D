#version 460 core

out vec4 color;

uniform vec3 lightColor;
uniform float lightRadius;
uniform float lightIntensity;
uniform float lightPosX;
uniform float lightPosY;

uniform sampler2D diffuse;

void main() {
	float length = length(vec2(lightPosX, lightPosY) - gl_FragCoord.xy);
	//convert distance from light origin to outer radius to range 0.0 to 1.0 for alpha
	float alpha = 1.0/(length/lightRadius);
	color = vec4(lightColor, clamp(alpha, 0.0, 1.0));
}