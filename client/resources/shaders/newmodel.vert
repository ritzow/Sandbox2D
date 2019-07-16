#version 410 core

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 textureCoord;

layout(location = 1) out vec2 passTextureCoord;

layout(packed) uniform InstanceData {
	mat4 transform2;
	float opacity2;
} idata;


/*layout(location = 0)*/ uniform mat4 transform;
//TODO does mat4 have the same float format as java/ieee? how to convert? could this cause the seam issues?
/*layout(location = 1)*/ uniform mat4 view;

void main() {
	gl_Position = view * transform /*instanceData.transform[gl_DrawID]*/ * vec4(position, 0.0, 1.0);
	passTextureCoord = textureCoord;
	//passOpacity = instanceData.opacity[gl_DrawID];
}