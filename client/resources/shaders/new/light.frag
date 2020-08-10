#version 460 core

layout(location = 0) in smooth vec2 inPosition;
layout(location = 1) in flat vec3 inColor;
layout(location = 2) in smooth vec2 solidTextureCoord;
layout(location = 3) in flat vec2 textureCenter;
layout(location = 4) in flat float sampleScale;

layout(location = 0) out vec4 fragColor; //color attachment 0

layout(location = 1, binding = 0) uniform sampler2D solidTexture;

const float SAMPLE_DISTANCE = 0.001;
const float DISSIPATION_FACTOR = 0.0025;

void main() {
    //TODO still need to deal with aspect ratio issues
    //texture coords are fewer in one direction based on aspect ratio (usually fewer horizontally since window is wide)
    vec2 increment = normalize(solidTextureCoord - textureCenter) * SAMPLE_DISTANCE; //texture coord distance to increment
    int samples = int(distance(solidTextureCoord, textureCenter)/length(increment));
    float alpha = 1 - length(inPosition); //standard alpha without occlusion
    for(vec2 texCoord = textureCenter; alpha > 0 && samples > 0; texCoord += increment, samples--) {
        alpha -= texture(solidTexture, texCoord).a * DISSIPATION_FACTOR / sampleScale;
    }

    fragColor = vec4(inColor, alpha);
}
