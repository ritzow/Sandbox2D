#include <windows.h>
#include "../include/jni.h"

constexpr bool SHOW_CONSOLE = false;
constexpr UINT32 OPTION_COUNT = 6;

typedef struct {
	JavaVM* vm;
	JNIEnv* env;
	jint result;
} VMInfo;

typedef jint (JNICALL *FunCreateJavaVM)(JavaVM**, void**, void*); FunCreateJavaVM CreateJavaVM;

VMInfo InstantiateVM(JavaVMOption options[OPTION_COUNT]) {
	JavaVMInitArgs args;
	args.version = JNI_VERSION_10;
	args.ignoreUnrecognized = false;
	args.nOptions = OPTION_COUNT;
	args.options = options;
	VMInfo info;
	info.result = CreateJavaVM(&info.vm, (void**)& info.env, &args);
	return info;
}

const char* getErrorType(jint result) {
	switch (result) {
	case JNI_OK: return "Success";
	case JNI_ERR: return "Unknown error";
	case JNI_EDETACHED: return "Thread detached from the VM";
	case JNI_EVERSION: return "JNI version error";
	case JNI_ENOMEM: return "Not enough memory";
	case JNI_EEXIST: return "VM already created";
	case JNI_EINVAL: return "Invalid arguments";
	default: return "Unknown error";
	}
}

void DisplayError(const char* title, const char* message) {
	MessageBox(nullptr, message, title, MB_OK | MB_ICONERROR);
}

void RunGame(VMInfo info, LPSTR args) {
	jclass classMain = info.env->FindClass("ritzow/sandbox/client/StartClient");
	jmethodID methodMain = info.env->GetStaticMethodID(classMain, "start", "(Ljava/lang/String;)V");
	info.env->CallStaticVoidMethod(classMain, methodMain, info.env->NewStringUTF(""));
	info.vm->DestroyJavaVM();
}

INT WINAPI WinMain(_In_ HINSTANCE hInstance, _In_opt_ HINSTANCE hPrevInstance, _In_ LPSTR lpCmdLine, _In_ INT nShowCmd) {
	if (SHOW_CONSOLE) {
		AllocConsole();
		FILE* file;
		freopen_s(&file, "CONOUT$", "w", stdout);
		printf("Command Line Arguments: \"%s\"\n", lpCmdLine);
	}

	HMODULE JvmDLL = LoadLibrary("jvm/bin/server/jvm.dll");
	if (JvmDLL != nullptr) {
		CreateJavaVM = (FunCreateJavaVM)GetProcAddress(JvmDLL, "JNI_CreateJavaVM");
		JavaVMOption options[OPTION_COUNT];
		options[0].optionString = const_cast<char*>("--module-path=client;shared;lwjgl-glfw.jar;PNGDecoder.jar;lwjgl-opengl.jar;lwjgl-openal.jar;lwjgl.jar;");
		options[1].optionString = const_cast<char*>("-Djava.class.path=lwjgl-glfw-natives-windows.jar;lwjgl-natives-windows.jar;lwjgl-openal-natives-windows.jar;lwjgl-opengl-natives-windows.jar");
		options[2].optionString = const_cast<char*>("-Djdk.module.main=ritzow.sandbox.client");
		options[3].optionString = const_cast<char*>("-Djdk.module.main.class=ritzow.sandbox.client.StartClient");
		options[4].optionString = const_cast<char*>("--enable-preview");
		options[5].optionString = const_cast<char*>("vfprintf");
		options[5].extraInfo = *vfprintf;
		VMInfo info = InstantiateVM(options);
		if (info.result == JNI_OK) {
			RunGame(info, lpCmdLine);
			FreeLibrary(JvmDLL);
			return EXIT_SUCCESS;
		} else {
			char dest;
			const char* str1 = "Could not create JVM: ";
			const char* str2 = getErrorType(info.result);
			size_t length = strlen(str1) + strlen(str2) + 1;
			strcpy_s(&dest, length, str1);
			strcat_s(&dest, length, str2);
			DisplayError("Java Virtual Machine Creation Failed", &dest);
			FreeLibrary(JvmDLL);
			return EXIT_FAILURE;
		}
	} else {
		DisplayError("Java Libray Load Failed", "Failed to load jvm.dll");
		return EXIT_FAILURE;
	}
}