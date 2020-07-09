#version 450 core

layout(location = 0) in float opacityPass;
layout(location = 1) in vec2 passTextureCoord;
layout(location = 0) out vec4 pixelColor;

uniform sampler2D atlasTexture;

void main() {
	vec4 color = texture(atlasTexture, passTextureCoord);
	color.a = color.a * opacityPass;
	pixelColor = color;
}