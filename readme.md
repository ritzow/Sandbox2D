<<<<<<< HEAD
#Sandbox2D
A 2D block building and destruction platformer game written in Java that uses OpenGL for graphics, OpenAL for audio, GLFW for cross-platform windowing and input, and LWJGL for bindings to the afformentioned libraries and APIs.
=======
# Sandbox2D

A 2D block building and destruction platformer game written in Java that uses OpenGL for graphics, OpenAL for audio, GLFW for cross-platform windowing and input, and LWJGL for bindings to the afformentioned libraries and APIs.   

## Libraries
### LWJGL
https://www.lwjgl.org/download   
Provides bindings to GLFW, OpenGL, and OpenAL. The program currently uses version 3.1.2 build 29. Just add each jar file to the classpath and LWJGL will load the natives automatically.

#### Configuration: 
```json
{
  "build": "release",
  "mode": "zip",
  "selectedAddons": [],
  "platform": [
    "windows",
    "macos",
    "linux"
  ],
  "descriptions": false,
  "compact": false,
  "hardcoded": false,
  "javadoc": true,
  "source": true,
  "language": "groovy",
  "contents": [
    "lwjgl",
    "lwjgl-glfw",
    "lwjgl-openal",
    "lwjgl-opengl"
  ],
  "version": "3.1.2",
  "versionLatest": "3.1.2"
}
```
### TWL's PNGDecoder:
http://twl.l33tlabs.org/dist/   
Used for loading textures from files so they can be sent to OpenGL. Just add to the classpath.
>>>>>>> origin/master
