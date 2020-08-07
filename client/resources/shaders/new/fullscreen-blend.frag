#version 460 core

layout(location = 0) in smooth vec2 textureCoord;
layout(location = 0) out vec4 fragColor; //color attachment 0

layout(location = 0, binding = 0) uniform sampler2D diffuse;
layout(location = 1, binding = 1) uniform sampler2D lighting;

void main() {
    fragColor = texture(lighting, textureCoord) * texture(diffuse, textureCoord);
}
