#version 450 core

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 textureCoord;
layout(location = 1) out vec2 passTextureCoord;

layout(location = 0) uniform mat4 transform;
layout(location = 1) uniform mat4 view;

/* TODO use struct to pass in transform/model data
this is incorrect because this shader is for each individual vertex, not model, but the idea is similars
struct ModelData {
	int modelPositionsIndex;
	int textureCoordsIndex;
	mat4 transformation;
}

layout(location = 0) uniform struct ModelData model;

*/

void main() {
	gl_Position = view * transform * vec4(position, 0.0, 1.0);
	passTextureCoord = textureCoord;
}