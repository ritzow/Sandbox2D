#version 410 core

in vec2 position, textureCoord;
out vec2 passTextureCoord;
uniform mat4 view, transform;

void main() {
	gl_Position = view * transform * vec4(position, 0.0, 1.0);
	passTextureCoord = textureCoord;
}