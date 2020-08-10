#version 460 core

layout(location = 0) in smooth vec2 textureCoord;
layout(location = 0) out vec4 fragColor;//color attachment 0

layout(location = 0, binding = 0) uniform sampler2D diffuse;
layout(location = 1, binding = 1) uniform sampler2D lighting;
//TODO layout(location = 2, binding = 2) uniform sampler2D shadow;

const float pi2 = 6.28318530718;// Pi*2

// GAUSSIAN BLUR SETTINGS {{{
const float DIRECTIONS = 16.0;// BLUR DIRECTIONS (Default 16.0 - More is better but slower)
const float QUALITY = 4.0;// BLUR QUALITY (Default 4.0 - More is better but slower)
const float SIZE = 8.0;// BLUR SIZE (Radius) default 8
// GAUSSIAN BLUR SETTINGS }}}

const float DIVISOR = QUALITY * DIRECTIONS - 15.0;

void main() {

    //Gaussian blur from https://www.shadertoy.com/view/Xltfzj
//    vec2 radius = SIZE/vec2(1920, 1080);//Size/iResolution.xy;
//
//    // Pixel colour
//    vec4 color = texture(diffuse, textureCoord);
//    float alpha = color.a;
//
//    // Blur calculations (TODO this needs to eat into the diffuse/shadow textureand make edges more transparent)
//    for(float d = 0.0; d < pi2; d += pi2/DIRECTIONS) {
//        for(float i = 1.0/QUALITY; i <= 1.0; i += 1.0/QUALITY) {
//            //TODO if is completely transparent, don't use
//            color += texture(diffuse, textureCoord + vec2(cos(d), sin(d)) * radius * i);
//        }
//    }
//
//    color /= DIVISOR;
//    color.a = alpha; //removes soft edges that extend past opaque parts of texture

    //Output to screen
    fragColor =  texture(diffuse, textureCoord) * texture(lighting, textureCoord); //color * texture(lighting, textureCoord);
}
