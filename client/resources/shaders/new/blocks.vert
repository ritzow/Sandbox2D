#version 460 core

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 textureCoord;

layout(location = 0) out smooth vec2 outTextureCoord;

layout(location = 0) uniform mat4 view;
layout(location = 1) uniform uvec3 blockGrid; //left block X, bottom blockY, blocks width

const int MAX_RENDER_COUNT = 100;

layout(binding = 1, std140) uniform instance_data {
    int offsets[MAX_RENDER_COUNT];
};

void main() {
    //get the index into the viewport block-grid
    //of the current series of the same block, then add the index of the current block
    int index = offsets[gl_DrawID] + gl_InstanceID;
    uint blockX = index % blockGrid[2] + blockGrid[0];
    uint blockY = index / blockGrid[2] + blockGrid[1];
    gl_Position = view * vec4(position + vec2(float(blockX), float(blockY)), 0.0, 1.0);
    outTextureCoord = textureCoord;
}