#version 460 core

layout(location = 0) in smooth vec2 textureCoord;
layout(location = 0) out vec4 fragColor; //color attachment 0
layout(location = 0) uniform sampler2D source; //TODO maybe use an image instead of sampler to be more efficient

void main() {
    fragColor = texture(source, textureCoord);
}
