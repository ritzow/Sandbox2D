![Sandbox2D](https://i.imgur.com/le6qo26.png)

# Sandbox2D
A 2D block building and destruction platformer game written in Java that uses OpenGL for graphics, OpenAL for audio, GLFW for cross-platform windowing and input, and LWJGL for bindings to the afformentioned libraries and APIs.   

# Repository Info
There are three directories that contain the source code: shared (contains code shared by client and server), client (contains client side code), and server (contains server code). The .classpath and .project files contain metadata used by the Eclipse Java IDE. The 'client' folder also contains a directory called 'resources' which contains game assets as well as OpenGL shader code. 

## Libraries
### LWJGL
https://www.lwjgl.org/customize
Provides bindings to GLFW, OpenGL, and OpenAL. The program currently uses version 3.2.0. Just add each jar file to the classpath and LWJGL will load the natives automatically.

#### Configuration: 
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
http://twl.l33tlabs.org/dist/   
Used for loading textures from files so they can be sent to OpenGL. Just add to the classpath.
