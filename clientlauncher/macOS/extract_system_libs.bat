mkdir temp
7z e lib\Command_Line_Tools_macOS_10.14_for_Xcode_10.2.1.dmg -otemp "Command Line Developer Tools\Command Line Tools (macOS Mojave version 10.14).pkg"
7z e "temp\Command Line Tools (macOS Mojave version 10.14).pkg" -otemp "CLTools_SDK_macOS1014.pkg\Payload"
7z e temp\Payload -otemp
7z x temp\Payload~ -otemp
move "temp\Library\Developer\CommandLineTools\SDKs\MacOSX.sdk\System\Library\Frameworks" "Frameworks"
rmdir /S /Q temp

mkdir temp
7z e lib\Command_Line_Tools_for_Xcode_11_Beta_2.dmg -otemp "Command Line Developer Tools\Command Line Tools.pkg"
7z e "temp\Command Line Tools.pkg" -otemp "CLTools_macOS1015_SDK.pkg\Payload"
7z e temp\Payload -otemp
7z x temp\Payload~ -otemp
move "temp\Library\Developer\CommandLineTools\SDKs\MacOSX10.15.sdk\usr" "lib\usr"
rmdir /S /Q temp
PAUSE