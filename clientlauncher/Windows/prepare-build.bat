
::clean up previous version
rmdir /S /Q "include"
rmdir /S /Q "x64\Release\jvm"

::create a java runtime
"C:\Program Files\Java\jdk-12\bin\jlink.exe" ^
	--compress=2 ^
	--no-man-pages ^
	--strip-debug ^
	--add-modules java.base,jdk.unsupported ^
	--output "x64/Release/jvm"

::copy files required to compile
move "x64\Release\jvm\include" "include"
copy "include\win32" "include"
rmdir /S /Q "include\win32"

::TODO call Build.java and move the resulting files?
::copy required runtime code and resources
xcopy /Y "..\..\client\libraries\lwjgl\lwjgl.jar" "x64\Release\"
xcopy /Y "..\..\client\libraries\lwjgl\lwjgl-opengl.jar" "x64\Release\"
xcopy /Y "..\..\client\libraries\lwjgl\lwjgl-openal.jar" "x64\Release\"
xcopy /Y "..\..\client\libraries\lwjgl\lwjgl-glfw.jar" "x64\Release\"
xcopy /Y "..\..\client\libraries\PNGDecoder\PNGDecoder.jar" "x64\Release\"
xcopy /E /Y "..\..\client\resources" "x64\Release\resources\"
xcopy /E /Y "..\..\client\bin" "x64\Release\client\"
xcopy /E /Y "..\..\shared\bin" "x64\Release\shared\"
PAUSE
