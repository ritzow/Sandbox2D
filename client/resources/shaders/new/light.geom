#version 460 core

layout(points) in;
layout(triangle_strip, max_vertices = 4) out;

layout(location = 0) in vec2 position[]; //calculated in Java
layout(location = 1) in vec3 color[];
layout(location = 2) in float intensity[];

//interpolate relative position, nopserspective since its always a regular square
layout(location = 0) out noperspective vec2 relativePosition;
layout(location = 1) out flat vec3 outColor;

layout(location = 0) uniform mat4 view;

void main() {
    //TODO could use polygons to compute fewer fragments https://open.gl/geometry
    //Generate the actual geometry for the light from the position in screen space and light properties

    outColor = color[0];
    relativePosition = vec2(-2, 2);
    gl_Position = view * vec4(position[0] + vec2(-intensity[0], intensity[0]), 0, 1);
    EmitVertex();

    outColor = color[0];
    relativePosition = vec2(-2, -2);
    gl_Position = view * vec4(position[0] + vec2(-intensity[0], -intensity[0]), 0, 1);
    EmitVertex();

    outColor = color[0];
    relativePosition = vec2(2, 2);
    gl_Position = view * vec4(position[0] + vec2(intensity[0], intensity[0]), 0, 1);
    EmitVertex();

    outColor = color[0];
    relativePosition = vec2(2, -2);
    gl_Position = view * vec4(position[0] + vec2(intensity[0], -intensity[0]), 0, 1);
    EmitVertex();

    EndPrimitive();
}