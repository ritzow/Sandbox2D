"C:\Program Files\Java\jdk-12\bin\jlink.exe" --compress=2 --no-man-pages --strip-debug --add-modules java.base,jdk.unsupported --output jvm
xcopy "./jvm" "../x64/Release/jvm" /E
PAUSE
