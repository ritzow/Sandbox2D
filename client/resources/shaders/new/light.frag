#version 460 core

layout(location = 0) in noperspective vec2 inPosition;
layout(location = 1) in flat vec3 inColor;

layout(location = 0) out vec4 fragColor; //color attachment 0

void main() {
    fragColor = vec4(inColor, 1 - distance(inPosition, vec2(0, 0)));
}
