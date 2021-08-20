#version 460 core

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 textureCoord;

layout(location = 0) out smooth vec2 outTextureCoord;
layout(location = 1) out flat float opacity;
layout(location = 2) out flat float exposure;

layout(location = 0) uniform mat4 view;

struct instance {
	float opacity;
	float exposure;
	mat4 transform;
};

const int MAX_RENDER_COUNT = 100;

layout(binding = 1, std140) uniform instance_data { //TODO store the single view matrix in here as well?
	int offsets[MAX_RENDER_COUNT]; //TODO pack the offsets into vec4s for improved size (increasing upload speed)
	instance instances[MAX_RENDER_COUNT];
};

void main() {
	instance i = instances[offsets[gl_DrawID] + gl_InstanceID];
	gl_Position = view * i.transform * vec4(position, 0.0, 1.0);
	outTextureCoord = textureCoord;
	opacity = i.opacity;
	exposure = i.exposure;
}