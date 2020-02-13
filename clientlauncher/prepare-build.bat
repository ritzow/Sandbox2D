::Windows build
@echo off

set "os=Windows"
set "arch=x64"
set "os_arch=%os%\%arch%"
set "output=%os_arch%\Release\Output"
set "dev=%os_arch%\Development\Output"
set "client=..\client"
set "shared=..\shared"
set "launcher_shared=Shared"
set "lwjgl=%client%\libraries\lwjgl"
set "jvmdir=%output%\jvm"

@echo Removing previous build files
::clean up previous version
rmdir /S /Q "Windows\include"
rmdir /S /Q "%output%"
rmdir /S /Q "%dev%"
mkdir "%output%"

@echo Building java program
::Run javac and jlink
java "%launcher_shared%\src\ritzow\sandbox\build\Build.java" "%shared%" "%client%" "%output%"

if not exist "%jvmdir%" (
	pause
	exit /B
)

@echo Copying header files to include directory
::copy files required to compile
move "%output%\jvm\include" "%os%\include"
copy "%os%\include\win32\" "%os%\include"
rmdir /S /Q "%os%\include\win32"

@echo Deleting unnecessary jvm files
::delete unecessary files kept by jlink
del "%jvmdir%\bin\java.exe"
del "%jvmdir%\bin\javaw.exe"
del "%jvmdir%\bin\keytool.exe"
del "%jvmdir%\lib\jvm.lib"

@echo Copying native LWJGL libraries to output
::copy natives using 7-zip to extract dlls from jars
7z e "%lwjgl%\lwjgl-glfw-natives-windows.jar" -o%output% -bso0 -bsp0 "windows\%arch%\org\lwjgl\glfw\*.dll"
7z e "%lwjgl%\lwjgl-natives-windows.jar" -o%output% -bso0 -bsp0 "windows\%arch%\org\lwjgl\*.dll"
7z e "%lwjgl%\lwjgl-openal-natives-windows.jar" -o%output% -bso0 -bsp0 "windows\%arch%\org\lwjgl\openal\*.dll"
7z e "%lwjgl%\lwjgl-opengl-natives-windows.jar" -o%output% -bso0 -bsp0 "windows\%arch%\org\lwjgl\opengl\*.dll"

@echo Copying game files to output
::copy resources to output
xcopy /E /Y /Q "%client%\resources" "%output%\resources\"
xcopy /Y /Q "%client%\options.txt" "%output%\"

@echo Building release launcher
msbuild Sandbox2DClientLauncher.vcxproj -p:Platform=%arch%;Configuration=Release

::@echo Building development launcher
::clone for development Build
::xcopy /E /Y /Q "%os_arch%\Release" "%os_arch%\Development\"
::msbuild Sandbox2DClientLauncher.vcxproj -p:Platform=%arch%;Configuration=Development

@echo Copying licenses to Release output
move "%output%\jvm\legal" "%output%"
xcopy /Y /Q "%lwjgl%\*license*" "%output%\legal"
xcopy /Y /Q "%lwjgl%\LICENSE" "%output%\legal"
rename "%output%\legal\LICENSE" "lwjgl_license.txt"
xcopy /Y /Q "%client%\libraries\json\LICENSE" "%output%\legal"
rename "%output%\legal\LICENSE" "json_license.txt"

PAUSE
