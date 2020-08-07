#version 460 core

layout(location = 0) in vec2 position;
layout(location = 1) in vec3 color;
layout(location = 2) in float intensity;

layout(location = 0) out vec2 outPosition;
layout(location = 1) out vec3 outColor;
layout(location = 2) out float outIntensity;

void main() {
    outPosition = position;
    outColor = color;
    outIntensity = intensity;
}
