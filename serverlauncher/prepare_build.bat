::Windows build
@echo off

set "output=build"

@echo Delete previous build files
::clean up previous version
rmdir /S /Q "%output%"
mkdir "%output%"

set "server=..\server"
set "shared=..\shared"

@echo Running jlink
::create the java runtime TODO call Build.java and move the resulting files instead of relying on Eclipse IDE build for client/shared modules
"C:\Program Files\Java\jdk-12\bin\jlink.exe" ^
	--compress=2 ^
	--no-man-pages ^
	--module-path "%server%\bin;%shared%\bin;" ^
	--add-modules java.base,ritzow.sandbox.shared,ritzow.sandbox.server ^
	--output "%output%\jvm"

@echo Deleting unnecessary jlink output
::delete unecessary files kept by jlink
set "jvmbin=%output%\jvm\bin"
del "%jvmbin%\javaw.exe"
del "%jvmbin%\keytool.exe"
del "%output%\jvm\lib\jvm.lib"
rmdir /S /Q "%output%\jvm\include"

@echo Creating run script file
@echo jvm\bin\java.exe ^
--module-path '../server/bin;../shared/bin;' ^
--add-modules ritzow.sandbox.shared,ritzow.sandbox.server ^
--enable-preview ^
-m ritzow.sandbox.server/ritzow.sandbox.server.StartServer > %output%\run.bat

PAUSE
