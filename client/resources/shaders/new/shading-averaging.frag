#version 460 core

layout(location = 0) out uint shading;

layout(location = 0, binding = 0, r8ui) readonly uniform uimage2D solidMap;

const int SUNLIGHT_RADIUS_BLOCKS = 5;

void main() {
    //Iterate over circular group of points:
    //https://stackoverflow.com/questions/40779343/java-loop-through-all-pixels-in-a-2d-circle-with-center-x-y-and-radius
//    for (int i = y-r; i < y+r; i++) {
//        for (int j = x; (j-x)^2 + (i-y)^2 <= r^2; j--) {
//            //in the circle
//        }
//        for (int j = x+1; (j-x)*(j-x) + (i-y)*(i-y) <= r*r; j++) {
//            //in the circle
//        }
//    }

    //if imageLoad is outside boundary, will return 0
    //TODO instead of average, do a sum that weights by distance of light and clamps at fully lit, where one adjacent fully lit block is enough to outweigh all other blocks dark
    ivec2 blockPos = ivec2(int(gl_FragCoord.x), int(gl_FragCoord.y));
    uint sum = 0;
    for(int row = blockPos.y - SUNLIGHT_RADIUS_BLOCKS; row <= blockPos.y + SUNLIGHT_RADIUS_BLOCKS; row++) {
        for(int column = blockPos.x - SUNLIGHT_RADIUS_BLOCKS; column <= blockPos.x + SUNLIGHT_RADIUS_BLOCKS; column++) {
            sum += imageLoad(solidMap, ivec2(column, row)).r;
        }
    }

    int dimension = SUNLIGHT_RADIUS_BLOCKS * 2 + 1;

    shading = int(float(sum) / float(dimension * dimension) * 255);
}
