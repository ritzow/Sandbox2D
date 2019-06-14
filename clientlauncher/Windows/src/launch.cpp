#include <windows.h>
#include "../include/jni.h"
#include <string>
#include <vector>

constexpr bool SHOW_CONSOLE = false;
constexpr UINT32 OPTION_COUNT = 5;

const char* getErrorString(jint result) {
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

typedef struct {
	JavaVM* vm;
	JNIEnv* env;
	jint result;
} VMInfo;

VMInfo CreateJVM(HMODULE dll, std::vector<JavaVMOption> options) {
	JavaVMInitArgs args;
	args.version = JNI_VERSION_10;
	args.ignoreUnrecognized = false;
	args.nOptions = (jint)options.size();
	args.options = options.data();

	VMInfo info;
	typedef jint(JNICALL *LoadFunc)(JavaVM**, JNIEnv**, JavaVMInitArgs*);
	LoadFunc CreateJavaVM = (LoadFunc)GetProcAddress(dll, "JNI_CreateJavaVM");
	info.result = CreateJavaVM(&info.vm, &info.env, &args);
	return info;
}

void RunGame(JNIEnv* env, LPSTR args) {
	jclass classMain = env->FindClass("ritzow/sandbox/client/StartClient");
	jmethodID methodMain = env->GetStaticMethodID(classMain, "start", "(Ljava/lang/String;)V");
	env->CallStaticVoidMethod(classMain, methodMain, env->NewStringUTF(args));
}

INT WINAPI WinMain(_In_ HINSTANCE hInstance, _In_opt_ HINSTANCE hPrevInstance, _In_ LPSTR lpCmdLine, _In_ INT nShowCmd) {
	if constexpr (SHOW_CONSOLE) {
		AllocConsole();
		FILE* file;
		freopen_s(&file, "CONOUT$", "w", stdout);
		printf("Command Line Arguments: \"%s\"\n", lpCmdLine);
	}

	HMODULE dll = LoadLibrary("jvm\\bin\\server\\jvm.dll");
	if (dll != nullptr) {
		std::vector<JavaVMOption> arguments {
			{(char*)"--module-path=client;shared;lwjgl-glfw.jar;PNGDecoder.jar;lwjgl-opengl.jar;lwjgl-openal.jar;lwjgl.jar;"},
			{(char*)"-Djdk.module.main=ritzow.sandbox.client"},
			{(char*)"-Djdk.module.main.class=ritzow.sandbox.client.StartClient"},
			{(char*)"--enable-preview"},
			{(char*)"vfprintf", *vfprintf}
		};

		VMInfo info = CreateJVM(dll, arguments);
		if (info.result == JNI_OK) {
			RunGame(info.env, lpCmdLine);
			if (info.env->ExceptionCheck()) {
				info.env->ExceptionDescribe();
			}
			info.vm->DestroyJavaVM();
			FreeLibrary(dll);
			return EXIT_SUCCESS;
		} else {
			std::string message = "Could not create JVM: ";
			message += getErrorString(info.result);
			DisplayError("Java Virtual Machine Creation Failed", message.c_str());
			FreeLibrary(dll);
			return EXIT_FAILURE;
		}
	} else {
		DisplayError("Java Libray Load Failed", "Failed to load jvm.dll");
		return EXIT_FAILURE;
	}
}