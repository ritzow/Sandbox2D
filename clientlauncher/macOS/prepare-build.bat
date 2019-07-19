::macOS build

set "output=x64\Release\Output"

::clean up previous version
rmdir /S /Q "include"
rmdir /S /Q "%output%"
rmdir /S /Q "x64\Development\Output"
mkdir "%output%"

set "client=..\..\client"
set "shared=..\..\shared"
set "lwjgl=%client%\libraries\lwjgl"
set "jmods=lib\jdk-12.0.1.jdk\Contents\Home\jmods"

::create the java runtime TODO call Build.java 
::and move the resulting files instead of relying on 
::Eclipse IDE build for client/shared modules
"C:\Program Files\Java\jdk-12\bin\jlink.exe" ^
	--compress=2 ^
	--no-man-pages ^
	--strip-debug ^
	--endian little ^
	--module-path "%jmods%;%client%\bin;%shared%\bin;%lwjgl%\lwjgl.jar;%lwjgl%\lwjgl-glfw.jar;%lwjgl%\lwjgl-opengl.jar;%lwjgl%\lwjgl-openal.jar;" ^
	--add-modules java.base,jdk.unsupported,ritzow.sandbox.client,ritzow.sandbox.shared,org.lwjgl,org.lwjgl.glfw,org.lwjgl.opengl,org.lwjgl.openal ^
	--output "%output%\jvm"

::delete unecessary files kept by jlink
del "%output%\jvm\bin\keytool"

::copy files required to compile
move "%output%\jvm\include" "include"
copy "include\darwin" "include"
rmdir /S /Q "include\darwin"

::copy resources to output 
xcopy /E /Y /Q "%client%\resources" "%output%\resources\"
xcopy "src\run" "%output%\"

::copy natives
xcopy /E /Y "natives" "%output%\"

::clone for development Build
xcopy /E /Y /Q "x64\Release\Output" "x64\Development\"

::clang++ -I ..\lib\usr\include -target x86_64-apple-darwin-macho -stdlib="libc++" launch.cpp

PAUSE
