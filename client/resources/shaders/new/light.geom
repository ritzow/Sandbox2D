#version 460 core

const int NUM_SIDES = 10;
const float PI = 3.1415926;
const float INCREMENT = PI * 2.0 / NUM_SIDES;

layout(points) in;
layout(triangle_strip, max_vertices = NUM_SIDES * 3) out;

layout(location = 0) in vec2 position[]; //calculated in Java
layout(location = 1) in vec3 color[];
layout(location = 2) in float intensity[];

//interpolate relative position, nopserspective since its always a regular square
layout(location = 0) out smooth vec2 outPosition;
layout(location = 2) out smooth vec2 solidTextureCoord;

layout(location = 1) out flat vec3 outColor;
layout(location = 3) out flat vec2 textureCenter;
layout(location = 4) out flat float sampleScale;

layout(location = 3) uniform float scale;
layout(location = 0) uniform mat4 view; //TODO dont use a matrix, because rotation is unnecessary and only one scale is needed

void main() {
    //TODO could use polygons to compute fewer fragments https://open.gl/geometry
    //TODO could generate a polygon then do the raycasting in here to determine each vertex darkness then interpolate it in the fragment shader (fewer raycasts if done here)
    //Generate the actual geometry for the light from the position in world space and light properties

    //set flat values
//    outColor = color[0];
//    textureCenter = ((view * vec4(position[0], 0, 1)).xy + 1)/2;
//    sampleScale = scale; //length((view * vec4(0, 1, 0, 1)).y);
//
//    outPosition = vec2(-1, 1);
//    gl_Position = view * vec4(position[0] + vec2(-intensity[0], intensity[0]), 0, 1);
//    solidTextureCoord = (gl_Position.xy + 1)/2;
//    EmitVertex();
//
//    outPosition = vec2(-1, -1);
//    gl_Position = view * vec4(position[0] - vec2(intensity[0], intensity[0]), 0, 1);
//    solidTextureCoord = (gl_Position.xy + 1)/2;
//    EmitVertex();
//
//    outPosition = vec2(1, 1);
//    gl_Position = view * vec4(position[0] + vec2(intensity[0], intensity[0]), 0, 1);
//    solidTextureCoord = (gl_Position.xy + 1)/2;
//    EmitVertex();
//
//    outPosition = vec2(1, -1);
//    gl_Position = view * vec4(position[0] + vec2(intensity[0], -intensity[0]), 0, 1);
//    solidTextureCoord = (gl_Position.xy + 1)/2;
//    EmitVertex();
//
//    EndPrimitive();

    vec2 pos = position[0];
    float length = intensity[0];
    vec4 center = view * vec4(pos, 0, 1);
    outColor = color[0];
    textureCenter = (center.xy + 1)/2;
    sampleScale = scale;

    outPosition = vec2(cos(0), -sin(0));
    gl_Position = view * vec4(pos + length * outPosition, 0, 1);
    solidTextureCoord = (gl_Position.xy + 1)/2;
    EmitVertex();

    float angle = INCREMENT;
    for(int i = 0; i <= NUM_SIDES; angle += INCREMENT, i++) {
        outPosition = vec2(0, 0);
        gl_Position = center;
        solidTextureCoord = textureCenter;
        EmitVertex();

        outPosition = vec2(cos(angle), -sin(angle));
        gl_Position = view * vec4(pos + length * outPosition, 0, 1);
        solidTextureCoord = (gl_Position.xy + 1)/2;
        EmitVertex();

        EndPrimitive(); //End triangle

        EmitVertex(); //Start new triangle
    }
}