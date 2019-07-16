#version 410 core

layout(location = 1) in vec2 passTextureCoord;
//layout(location = 2) in float passOpacity;

layout(location = 0) out vec4 pixelColor;

uniform sampler2D textureSampler;
/*layout(location = 3)*/ uniform float opacity;

void main() {
	//TODO create shadow map and use some sort of sampler/uniform to write to it as well
	vec4 color = texture(textureSampler, passTextureCoord);
	color.a = color.a * opacity /*passOpacity*/;
	pixelColor = color;
}