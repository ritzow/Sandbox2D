#version 460 core

layout(points) in;
layout(triangle_strip, max_vertices = 4) out;

layout(location = 0) out smooth vec2 textureCoord;

void main() {
    textureCoord = vec2(0, 1);
    gl_Position = vec4(-1, 1, 0, 1);
    EmitVertex();

    textureCoord = vec2(0, 0);
    gl_Position = vec4(-1, -1, 0, 1);
    EmitVertex();

    textureCoord = vec2(1, 1);
    gl_Position = vec4(1, 1, 0, 1);
    EmitVertex();

    textureCoord = vec2(1, 0);
    gl_Position = vec4(1, -1, 0, 1);
    EmitVertex();

    EndPrimitive();
}
