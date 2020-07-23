#version 460 core

layout(location = 0) in float passOpacity;
layout(location = 1) in vec2 passTextureCoord;
layout(location = 2) in float passExposure;
layout(location = 0) out vec4 pixelColor;

uniform sampler2D atlasTexture;

void main() {
	vec4 texel = texture(atlasTexture, passTextureCoord);
	pixelColor = vec4(texel.rgb * passExposure, texel.a * passOpacity);
}