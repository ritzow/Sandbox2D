#include <windows.h>
#include <PathCch.h>
#include <iostream>
#include <shared.cpp>
#include <cwchar>

inline const bool SHOW_CONSOLE = false;

inline void DisplayError(const wchar_t* title, const wchar_t* message) {
	MessageBoxW(nullptr, message, title, MB_OK | MB_ICONERROR);
}

void SetWorkingDirectory(HMODULE module) {
	WCHAR program_path[MAX_PATH];
	DWORD length = GetModuleFileNameW(module, program_path, MAX_PATH);
	PathCchRemoveFileSpec(program_path, length);
	SetCurrentDirectoryW(program_path);
}

//void SetWorkingDirectory(HMODULE module) {
//	WCHAR program_path[MAX_PATH];
//	DWORD nSize = MAX_PATH;
//	DWORD length;
//	do {
//		length = GetModuleFileNameW(module, program_path, MAX_PATH);
//		if (GetLastError() == ERROR_INSUFFICIENT_BUFFER) {
//
//		}
//	} while (GetLastError() == ERROR_INSUFFICIENT_BUFFER);
//	PathCchRemoveFileSpec(program_path, length);
//	SetCurrentDirectoryW(program_path);
//}

void SetupConsole(LPWSTR args) {
	AllocConsole();
	FILE* file;
	_wfreopen_s(&file, L"CONOUT$", L"w", stdout);
	if (wcslen(args) > 0)
		std::wcout << "Program Arguments: \"" << args << '"' << std::endl;
}



INT WINAPI wWinMain(_In_ HMODULE module, _In_opt_ HINSTANCE, _In_ LPWSTR lpCmdLine, _In_ INT) {
	if constexpr (SHOW_CONSOLE) SetupConsole(lpCmdLine);
	SetWorkingDirectory(module);
	HMODULE dll = LoadLibraryW(LR"(jvm\bin\server\jvm.dll)");
	if (dll != nullptr) {
		JavaVM* vm; JNIEnv* env; JavaVMInitArgs vmargs = GetJavaInitArgs();
		jint result = ((StartJVM)GetProcAddress(dll, "JNI_CreateJavaVM"))(&vm, &env, &vmargs);
		if (result == JNI_OK) {
			RunGame(env, __argc - 1, __wargv + 1);
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