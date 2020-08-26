#version 460 core

//If I ever want multiple outputs:
//https://stackoverflow.com/questions/30025009/in-opengl-is-it-possible-for-one-shader-program-in-one-draw-call-to-render-to?rq=1

layout(location = 0) in smooth vec2 textureCoord;

layout(location = 0) out vec4 color; //color attachment 0

layout(location = 2, binding = 0) uniform sampler2D atlasTexture;
layout(location = 3) uniform float exposure;

void main() {
    vec4 texel = texture(atlasTexture, textureCoord);
    color = vec4(texel.rgb * exposure, texel.a);
}