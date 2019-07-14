#include <windows.h>
#include "../../Shared/shared.cpp"

constexpr bool SHOW_CONSOLE = false;

inline void DisplayError(const wchar_t* title, const wchar_t* message) {
	MessageBoxW(nullptr, message, title, MB_OK | MB_ICONERROR);
}

INT WINAPI wWinMain(_In_ HINSTANCE hInstance, _In_opt_ HINSTANCE hPrevInstance, 
	_In_ LPWSTR lpCmdLine, _In_ INT nShowCmd) {
	if constexpr (SHOW_CONSOLE) {
		AllocConsole();
		FILE* file;
		_wfreopen_s(&file, L"CONOUT$", L"w", stdout);
		wprintf(L"Command Line Arguments: \"%s\"\n", lpCmdLine);
	}

	HMODULE dll = LoadLibraryW(L"jvm\\bin\\server\\jvm.dll");
	if (dll != nullptr) {
		JavaVM* vm; JNIEnv* env;
		jint result = ((CreateJavaVM)GetProcAddress(dll, "JNI_CreateJavaVM"))
			(&vm, &env, &GetJavaInitArgs());

		if (result == JNI_OK) {
			RunGame(env, lpCmdLine);
			vm->DestroyJavaVM();
			FreeLibrary(dll);
			return EXIT_SUCCESS;
		} else {
			std::wstring message = L"Could not create JVM: ";
			message += getErrorString(result);
			DisplayError(L"Java Virtual Machine Creation Failed", message.c_str());
			FreeLibrary(dll);
			return EXIT_FAILURE;
		}
	} else {
		DisplayError(L"Java Libray Load Failed", L"Failed to load jvm.dll");
		return EXIT_FAILURE;
	}
}