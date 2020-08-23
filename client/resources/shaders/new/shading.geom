#version 460 core

layout(points) in;
layout(triangle_strip, max_vertices = 4) out;

layout(location = 0) out smooth vec2 textureCoord;

layout(location = 1) uniform vec4 worldBounds;

void main() {
    //TODO make these not fullscreen, only the viewed rectangle
    gl_Position = vec4(-1, 1, 0, 1);
    EmitVertex();

    gl_Position = vec4(-1, -1, 0, 1);
    EmitVertex();

    gl_Position = vec4(1, 1, 0, 1);
    EmitVertex();

    gl_Position = vec4(1, -1, 0, 1);
    EmitVertex();

    EndPrimitive();
}
