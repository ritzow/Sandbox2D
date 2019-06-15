set "output=x64\Release\Output"

::clean up previous version
rmdir /S /Q "include"
rmdir /S /Q "%output%"
rmdir /S /Q "x64\Development\Output"
mkdir "%output%"

::create the java runtime
"C:\Program Files\Java\jdk-12\bin\jlink.exe" ^
	--compress=1 ^
	--no-man-pages ^
	--strip-debug ^
	--endian little ^
	--add-modules java.base,jdk.unsupported ^
	--output "%output%\jvm"

::delete unecessary files kept by jlink
set "jvmbin=%output%\jvm\bin"
del "%jvmbin%\java.exe"
del "%jvmbin%\javaw.exe"
del "%jvmbin%\keytool.exe"
del "%output%\jvm\lib\jvm.lib"

::copy files required to compile
move "%output%\jvm\include" "include"
copy "include\win32" "include"
rmdir /S /Q "include\win32"

set "client=..\..\client"
set "shared=..\..\shared"
set "lwjgl=%client%\libraries\lwjgl"

::copy code, libraries, and resources to output TODO call Build.java and move the resulting files instead of relying on Eclipse IDE build
xcopy /E /Y /Q "%client%\bin" "%output%\client\"
xcopy /E /Y /Q "%shared%\bin" "%output%\shared\"
xcopy /E /Y /Q "%client%\resources" "%output%\resources\"
xcopy /Y "%lwjgl%\lwjgl.jar" "%output%\"
xcopy /Y "%lwjgl%\lwjgl-opengl.jar" "%output%\"
xcopy /Y "%lwjgl%\lwjgl-openal.jar" "%output%\"
xcopy /Y "%lwjgl%\lwjgl-glfw.jar" "%output%\"
xcopy /Y "%client%\libraries\PNGDecoder\PNGDecoder.jar" "%output%\"

::copy natives
xcopy /E /Y "natives" "%output%\"

::clone for development Build
xcopy /E /Y /Q "x64\Release" "x64\Development\"

PAUSE
