#version 460 core

layout(points) in;
layout(triangle_strip, max_vertices = 4) out;

layout(location = 0) out smooth vec2 worldCoord;

layout(location = 0) uniform vec4 view; //left x, bottom y, right x, top y

void main() {
    worldCoord = vec2(view[0], view[3]);
    gl_Position = vec4(-1, 1, 0, 1);
    EmitVertex();

    worldCoord = vec2(view[0], view[1]);
    gl_Position = vec4(-1, -1, 0, 1);
    EmitVertex();

    worldCoord = vec2(view[2], view[3]);
    gl_Position = vec4(1, 1, 0, 1);
    EmitVertex();

    worldCoord = vec2(view[2], view[1]);
    gl_Position = vec4(1, -1, 0, 1);
    EmitVertex();

    EndPrimitive();
}
