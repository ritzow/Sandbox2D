glslangValidator -Os -G460 -g -e main --target-env opengl resources/shaders/new/model.vert -o resources/shaders/new/out/model.vert.spv
glslangValidator -Os -G460 -g -e main --target-env opengl resources/shaders/new/model.frag -o resources/shaders/new/out/model.frag.spv

glslangValidator -Os -G460 -g -e main --target-env opengl resources/shaders/new/light.vert -o resources/shaders/new/out/light.vert.spv
glslangValidator -Os -G460 -g -e main --target-env opengl resources/shaders/new/light.geom -o resources/shaders/new/out/light.geom.spv
glslangValidator -Os -G460 -g -e main --target-env opengl resources/shaders/new/light.frag -o resources/shaders/new/out/light.frag.spv

glslangValidator -Os -G460 -g -e main --target-env opengl resources/shaders/new/fullscreen.vert -o resources/shaders/new/out/fullscreen.vert.spv
glslangValidator -Os -G460 -g -e main --target-env opengl resources/shaders/new/fullscreen.geom -o resources/shaders/new/out/fullscreen.geom.spv
glslangValidator -Os -G460 -g -e main --target-env opengl resources/shaders/new/fullscreen.frag -o resources/shaders/new/out/fullscreen.frag.spv
glslangValidator -Os -G460 -g -e main --target-env opengl resources/shaders/new/fullscreen-blend.frag -o resources/shaders/new/out/fullscreen-blend.frag.spv