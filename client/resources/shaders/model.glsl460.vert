#version 460 core

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 textureCoord;
layout(location = 1) out vec2 passTextureCoord;
layout(location = 0) out float opacity;
layout(location = 1) uniform mat4 view;

struct instance {
	float opacity;
	mat4 transform;
};

layout(binding = 1, std140) uniform instance_data { //TODO store the single view matrix in here as well
	int offsets[8];
	instance instances[64 * 8]; //8 4x4 matrices
};

void main() {
	instance i = instances[offsets[gl_DrawID] + gl_InstanceID];
	gl_Position = view * i.transform * vec4(position, 0.0, 1.0);
	passTextureCoord = textureCoord;
	opacity = i.opacity;
}