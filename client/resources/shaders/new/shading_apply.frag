#version 460 core

layout(location = 0) in smooth vec2 worldCoord;

layout(location = 0) out vec4 color;

layout(location = 1, binding = 0) uniform usampler2D tileLighting;

float lighting(ivec2 position) {
    return 1.0 - texelFetch(tileLighting, position, 0).r/255.0;
    //ivec2(int(worldCoord.x) + offsetX, int(worldCoord.y) + offsetY)
}

void main() {
    //TODO blur/interpolate/smoothstep multiple texelFetches together to create smoother lighting
    //maybe smoothstep or lerp between the four adjacent blocks?
    //uint shading = texelFetch(tileLighting, ivec2(int(worldCoord.x), int(worldCoord.y)), 0).r;
    float ratioX = fract(worldCoord.x);
    float ratioY = fract(worldCoord.y);
    float bottom = mix(lighting(ivec2(floor(worldCoord.x), floor(worldCoord.y))), lighting(ivec2(ceil(worldCoord.x), floor(worldCoord.y))), ratioX);
    float top = mix(lighting(ivec2(floor(worldCoord.x), ceil(worldCoord.y))), lighting(ivec2(ceil(worldCoord.x), ceil(worldCoord.y))), ratioX);
    color = vec4(0, 0, 0, mix(bottom, top, ratioY));
}