![Sandbox2D](https://i.imgur.com/8GxCB4C.png)

# Sandbox2D
A 2D block building and destruction platformer game written in Java that uses OpenGL for graphics, OpenAL for audio, GLFW for cross-platform windowing and input, and LWJGL for bindings to the afformentioned libraries and APIs.

# Repository Structure
**/shared**: Contains code shared by client and server *(module ritzow.sandbox.shared)*.

**/client**: Contains client-side code and resources including game assets and OpenGL shader code *(module ritzow.sandbox.client)*.

**/clientlauncher**: native launcher and client build script.

**/server**: The game server *(module ritzow.sandbox.server)*.

**/serverlauncher**: JavaFX dedicated server GUI *(module ritzow.sandbox.serverlauncher)*.

# Libraries
### [LWJGL](https://www.lwjgl.org/customize)

Provides bindings to GLFW, OpenGL, and OpenAL.

#### Download Configuration
```json
{
    "build": "stable",
    "mode": "zip",
    "platform": [
        "windows",
        "macos",
        "linux"
    ],
    "javadoc": true,
    "source": true,
    "contents": [
        "lwjgl",
        "lwjgl-glfw",
        "lwjgl-opengl",
        "lwjgl-openal"
    ]
}
```
### TWL's PNGDecoder:
Used for loading textures. Does not seem to be available directly anymore as the official website seems to be in use by scammers. A modified copy is included in the client module (and other libraries including slick-util also include a copy).

### [JSON-java](https://github.com/stleary/JSON-java)

A simple JSON parser reference implementation used for loading GLTF files in the client (not actually implemented).

### [JavaFX](https://openjfx.io/)

Used for the server launcher. Use the latest version.

### [Apache Commons Validator](http://commons.apache.org/proper/commons-validator/)

Used to provide intuitive error display in the server launcher.
