#include <shared.cpp>
#include <windows.h>
#include <PathCch.h>
#include <iostream>

constexpr bool SHOW_CONSOLE = false;

inline void DisplayError(const wchar_t* title, const wchar_t* message) {
	MessageBoxW(nullptr, message, title, MB_OK | MB_ICONERROR);
}

INT WINAPI wWinMain(_In_ HINSTANCE module, _In_opt_ HINSTANCE, _In_ LPWSTR lpCmdLine, _In_ INT) {
	if constexpr (SHOW_CONSOLE) {
		AllocConsole();
		FILE* file;
		_wfreopen_s(&file, L"CONOUT$", L"w", stdout);
		if(wcslen(lpCmdLine) > 0)
			std::wcout << "Command Line Arguments: \"" << lpCmdLine << '"' << std::endl;
	}

	static WCHAR program_path[MAX_PATH];
	DWORD length = GetModuleFileNameW(module, program_path, MAX_PATH);
	PathCchRemoveFileSpec(program_path, length);
	SetCurrentDirectoryW(program_path);
	HMODULE dll = LoadLibraryW(LR"(jvm\bin\server\jvm.dll)");
	if (dll != nullptr) {
		JavaVM* vm; JNIEnv* env; JavaVMInitArgs args = GetJavaInitArgs();
		jint result = ((StartJVM)GetProcAddress(dll, "JNI_CreateJavaVM"))(&vm, &env, &args);
		if (result == JNI_OK) {
			RunGame(env, lpCmdLine);
			vm->DestroyJavaVM();
			FreeLibrary(dll);
			return EXIT_SUCCESS;
		} else {
			std::wstring message = L"Could not create JVM: ";
			message += GetErrorString(result);
			DisplayError(L"Java Virtual Machine Creation Failed", message.c_str());
			FreeLibrary(dll);
			return EXIT_FAILURE;
		}
	} else {
		DisplayError(L"Java Libray Load Failed", L"Failed to load jvm.dll");
		return EXIT_FAILURE;
	}
}