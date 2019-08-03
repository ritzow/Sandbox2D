::macOS build
@echo off

set "output=x64\Release\Output"

::clean up previous version
@echo Removing previous build files
rmdir /S /Q "include"
rmdir /S /Q "%output%"
rmdir /S /Q "x64\Development\Output"
mkdir "%output%"

set "client=..\..\client"
set "shared=..\..\shared"
set "lwjgl=%client%\libraries\lwjgl"

::create the java runtime TODO call Build.java 
::and move the resulting files instead of relying on 
::Eclipse IDE build for client/shared modules
@echo Running jlink
"C:\Program Files\Java\jdk-12\bin\jlink.exe" ^
	--compress=2 ^
	--no-man-pages ^
	--strip-debug ^
	--endian little ^
	--module-path "lib\jmods;%client%\bin;%shared%\bin;%lwjgl%\lwjgl.jar;%lwjgl%\lwjgl-glfw.jar;%lwjgl%\lwjgl-opengl.jar;%lwjgl%\lwjgl-openal.jar;" ^
	--add-modules java.base,jdk.unsupported,ritzow.sandbox.client,ritzow.sandbox.shared,org.lwjgl,org.lwjgl.glfw,org.lwjgl.opengl,org.lwjgl.openal ^
	--output "%output%\jvm"

::delete unecessary files kept by jlink
@echo Deleting unnecessary files
del "%output%\jvm\bin\keytool"

::copy files required to compile
@echo Moving header files
move "%output%\jvm\include" "lib\include"
copy "lib\include\darwin" "include"
rmdir /S /Q "lib\include\darwin"

::copy resources to output 
@echo Copying game resources and launcher to output
xcopy /E /Y /Q "%client%\resources" "%output%\resources\"
xcopy "src\run.command" "%output%\"

::copy natives
@echo Copying natives libraries
xcopy /E /Y "lib\natives" "%output%\"

::create zipped program archive
@echo Write output to archive
7z a -tzip -mx1 sandbox2d.zip ".\%output%\*"

::clang++ -I ..\lib\usr\include -target x86_64-apple-darwin-macho -stdlib="libc++" launch.cpp

PAUSE
