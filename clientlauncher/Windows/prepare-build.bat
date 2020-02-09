::Windows build
@echo off

set "arch=x64"

set "output=%arch%\Release\Output"

@echo Removing previous build files
::clean up previous version
rmdir /S /Q "include"
rmdir /S /Q "%output%"
rmdir /S /Q "%arch%\Development\Output"
mkdir "%output%"

set "client=..\..\client"
set "shared=..\..\shared"
set "lwjgl=%client%\libraries\lwjgl"
set "jvmdir=%output%\jvm"

::Run javac and jlink
"%JAVA_HOME%\bin\java.exe" --source 14 ^
"..\Shared\src\ritzow\sandbox\build\Build.java" "..\..\shared" "..\..\client" "%output%"

if not exist "%jvmdir%" (
	pause 
	exit /B
)

@echo Deleting unnecessary jvm files
::delete unecessary files kept by jlink
del "%jvmdir%\bin\java.exe"
del "%jvmdir%\bin\javaw.exe"
del "%jvmdir%\bin\keytool.exe"
del "%jvmdir%\lib\jvm.lib"

@echo Copying header files to include directory
::copy files required to compile
move "%output%\jvm\include" "include"
copy "include\win32" "include"
rmdir /S /Q "include\win32"

@echo Copying game files to output
::copy resources to output
xcopy /E /Y /Q "%client%\resources" "%output%\resources\"
xcopy /Y /Q "%client%\options.txt" "%output%\"

@echo Copying native LWJGL libraries to output
::copy natives using 7-zip to extract dlls from jars
7z e "%lwjgl%\lwjgl-glfw-natives-windows.jar" -o%output% -bso0 -bsp0 "windows\%arch%\org\lwjgl\glfw\*.dll"
7z e "%lwjgl%\lwjgl-natives-windows.jar" -o%output% -bso0 -bsp0 "windows\%arch%\org\lwjgl\*.dll"
7z e "%lwjgl%\lwjgl-openal-natives-windows.jar" -o%output% -bso0 -bsp0 "windows\%arch%\org\lwjgl\openal\*.dll"
7z e "%lwjgl%\lwjgl-opengl-natives-windows.jar" -o%output% -bso0 -bsp0 "windows\%arch%\org\lwjgl\opengl\*.dll"

@echo Copying Release output to Development output
::clone for development Build
xcopy /E /Y /Q "%arch%\Release" "%arch%\Development\"

@echo Copying licenses to Release output
move "%output%\jvm\legal" "%output%"
xcopy /Y /Q "%lwjgl%\*license*" "%output%\legal"
xcopy /Y /Q "%lwjgl%\LICENSE" "%output%\legal"
rename "%output%\legal\LICENSE" "lwjgl_license.txt"
xcopy /Y /Q "%client%\libraries\json\LICENSE" "%output%\legal"
rename "%output%\legal\LICENSE" "json_license.txt"

PAUSE
