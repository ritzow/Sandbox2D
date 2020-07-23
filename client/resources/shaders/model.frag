#version 410 core

in vec2 passTextureCoord;
out vec4 pixelColor;
uniform sampler2D atlasTexture;
uniform float opacity, exposure;

void main() {
	vec4 color = texture(atlasTexture, passTextureCoord);
	pixelColor = vec4(color.rgb * exposure, color.a * opacity);
}