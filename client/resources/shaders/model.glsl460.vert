#version 460 core

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 textureCoord;
layout(location = 0) out float passOpacity;
layout(location = 1) out vec2 passTextureCoord;
layout(location = 2) out float passExposure;
layout(location = 1) uniform mat4 view;

struct instance {
	float opacity;
	float exposure;
	mat4 transform;
};

const int MAX_RENDER_COUNT = 600;

layout(binding = 1, std140) uniform instance_data { //TODO store the single view matrix in here as well?
	int offsets[MAX_RENDER_COUNT]; //TODO pack the offsets into vec4s for improved size (increasing upload speed)
	instance instances[MAX_RENDER_COUNT];
};

void main() {
	instance i = instances[offsets[gl_DrawID] + gl_InstanceID];
	gl_Position = view * i.transform * vec4(position, 0.0, 1.0);
	passTextureCoord = textureCoord;
	passOpacity = i.opacity;
	passExposure = i.exposure;
}