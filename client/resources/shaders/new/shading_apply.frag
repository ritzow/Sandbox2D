#version 460 core

layout(location = 0) in smooth vec2 worldCoord;

layout(location = 0) out vec4 color;

layout(location = 1, binding = 0) uniform usampler2D tileLighting;

const bool SMOOTH_LIGHTING = true;

float lighting(ivec2 position) {
    return 1.0 - texelFetch(tileLighting, position, 0).r/255.0;
}

void main() {
    if(SMOOTH_LIGHTING) {
        float ratioX = fract(worldCoord.x);
        float ratioY = fract(worldCoord.y);
        int left = int(floor(worldCoord.x));
        int right = int(ceil(worldCoord.x));
        int bottom = int(floor(worldCoord.y));
        int top = int(ceil(worldCoord.y));
        float lightBottom = mix(lighting(ivec2(left, bottom)), lighting(ivec2(right, bottom)), ratioX);
        float lightTop = mix(lighting(ivec2(left, top)), lighting(ivec2(right, top)), ratioX);
        color = vec4(0, 0, 0, mix(lightBottom, lightTop, ratioY));
    } else {
        color = vec4(0, 0, 0, lighting(ivec2(int(worldCoord.x + 0.5), int(worldCoord.y + 0.5))));
    }
}