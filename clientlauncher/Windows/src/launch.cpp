#include <windows.h>
#include "../jvm/include/jni.h"

void CreateConsole() {
	AllocConsole();
	FILE* file;
	freopen_s(&file, "CONOUT$", "w", stdout);
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

constexpr UINT32 OPTION_COUNT = 5;

typedef struct {
	JavaVM* vm;
	JNIEnv* env;
	jint result;
} VMInfo;

VMInfo CreateVM(JavaVMOption options[OPTION_COUNT]) {
	JavaVMInitArgs args;
	args.version = JNI_VERSION_10;
	args.ignoreUnrecognized = FALSE;
	args.nOptions = OPTION_COUNT;
	args.options = options;
	VMInfo info;
	info.result = JNI_CreateJavaVM(&info.vm, (void**)&info.env, &args);
	return info;
}

void concat(const char* str1, const char* str2, char* dest) {
	size_t length = strlen(str1) + strlen(str2) + 1;
	if (dest != nullptr) {
		strcpy_s(dest, length, str2);
		strcat_s(dest, length, str1);
	}
}

INT WINAPI WinMain(_In_ HINSTANCE hInstance, _In_opt_ HINSTANCE hPrevInstance, _In_ LPSTR lpCmdLine, _In_ INT nShowCmd) {
	//CreateConsole();
	JavaVMOption options[OPTION_COUNT];
	options[0].optionString = "--module-path=client;shared;lwjgl-glfw.jar;PNGDecoder.jar;lwjgl-opengl.jar;lwjgl-openal.jar;lwjgl.jar;";
	options[1].optionString = "-Djava.class.path=lwjgl-glfw-natives-windows.jar;lwjgl-natives-windows.jar;lwjgl-openal-natives-windows.jar;lwjgl-opengl-natives-windows.jar";
	options[2].optionString = "-Djdk.module.main=ritzow.sandbox.client";
	options[3].optionString = "-Djdk.module.main.class=ritzow.sandbox.client.StartClient";
	options[4].optionString = "--enable-preview";
	VMInfo info = CreateVM(options);
	if (info.result == JNI_OK) {
		jclass classMain = info.env->FindClass("ritzow/sandbox/client/StartClient");
		jclass classString = info.env->FindClass("java/lang/String");
		jmethodID methodMain = info.env->GetStaticMethodID(classMain, "start", "(Ljava/lang/String;)V");
		info.env->CallStaticVoidMethod(classMain, methodMain, info.env->NewStringUTF(lpCmdLine));
		info.vm->DestroyJavaVM();
		return 0;
	} else {
		char dest;
		concat(getErrorType(info.result), "Could not create JVM: ", &dest);
		MessageBox(nullptr, &dest, "Java Virtual Machine Creation Failed", MB_OK | MB_ICONERROR);
		return 1;
	}
}