#version 450 core

layout(location = 1) in vec2 passTextureCoord;
layout(location = 0) out vec4 pixelColor;

uniform sampler2D textureSampler;
layout(location = 3) uniform float opacity;

void main() {
	vec4 color = texture(textureSampler, passTextureCoord);
	color.a = color.a * opacity;
	pixelColor = color;
}