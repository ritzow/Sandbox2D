#include <windows.h>
#include <string>
#include <sstream>
#include <vector>
#include "../include/jni.h"

constexpr bool SHOW_CONSOLE = false;

std::vector<JavaVMOption> vmOptions {
	{const_cast<char*>("-Djdk.module.main=ritzow.sandbox.client")},
	{const_cast<char*>("-Djdk.module.main.class=ritzow.sandbox.client.StartClient")},
	{const_cast<char*>("--enable-preview")}, //Java 12 switch expressions
	{const_cast<char*>("vfprintf"), *vfprintf}
};

const wchar_t* getErrorString(jint result) noexcept {
	switch (result) {
	case JNI_OK: return L"Success";
	case JNI_ERR: return L"Unknown error";
	case JNI_EDETACHED: return L"Thread detached from the VM";
	case JNI_EVERSION: return L"JNI version error";
	case JNI_ENOMEM: return L"Not enough memory";
	case JNI_EEXIST: return L"VM already created";
	case JNI_EINVAL: return L"Invalid arguments";
	default: return L"Unknown error";
	}
}

inline void DisplayError(const wchar_t* title, const wchar_t* message) {
	MessageBoxW(nullptr, message, title, MB_OK | MB_ICONERROR);
}

void DisplayException(JNIEnv* env) {
	jthrowable exception = env->ExceptionOccurred();
	jclass throwableClass = env->FindClass("java/lang/Throwable");
	jclass elementClass = env->FindClass("java/lang/StackTraceElement");

	jmethodID throwableToString = env->GetMethodID(throwableClass,
		"toString", "()Ljava/lang/String;");
	jmethodID getStackTrace = env->GetMethodID(throwableClass,
		"getStackTrace", "()[Ljava/lang/StackTraceElement;");
	jmethodID elementToString = env->GetMethodID(elementClass,
		"toString", "()Ljava/lang/String;");
	env->DeleteLocalRef(throwableClass);
	env->DeleteLocalRef(elementClass);

	jstring msgString = (jstring)env->CallObjectMethod(exception, throwableToString);
	jobjectArray trace = (jobjectArray)env->CallObjectMethod(exception, getStackTrace);
	const char* message = env->GetStringUTFChars(msgString, nullptr);
	std::wstringstream builder;
	builder << message;
	env->ReleaseStringUTFChars(msgString, message);
	env->DeleteLocalRef(msgString);
	jsize length = env->GetArrayLength(trace);
	for (int i = 0; i < length; i++) {
		jobject element = env->GetObjectArrayElement(trace, i);
		jstring msg = static_cast<jstring>(env->CallObjectMethod(element, elementToString));
		const char* str = env->GetStringUTFChars(msg, nullptr);
		builder << L"\n	at " << str;
		env->ReleaseStringUTFChars(msg, str);
		env->DeleteLocalRef(msg);
	}
	env->DeleteLocalRef(trace);
	DisplayError(L"Java Exception Occurred on Main Thread", builder.str().c_str());
}

typedef struct {
	JavaVM* vm;
	JNIEnv* env;
	jint result;
} VMInfo;

VMInfo CreateJVM(HMODULE dll) {
	JavaVMInitArgs args;
	args.version = JNI_VERSION_10;
	args.ignoreUnrecognized = false;
	args.nOptions = (jint)vmOptions.size();
	args.options = vmOptions.data();

	typedef jint(JNICALL *LoadFunc)(JavaVM**, JNIEnv**, JavaVMInitArgs*);
	LoadFunc CreateJavaVM = (LoadFunc)GetProcAddress(dll, "JNI_CreateJavaVM");
	VMInfo info;
	info.result = CreateJavaVM(&info.vm, &info.env, &args);
	return info;
}

void RunGame(JNIEnv* env, LPWSTR windowsArgs) {
	jclass classMain = env->FindClass("ritzow/sandbox/client/StartClient");
	jmethodID methodMain = env->GetStaticMethodID(classMain, "start", "(Ljava/lang/String;)V");
	jstring args = env->NewString((jchar*)windowsArgs, wcslen(windowsArgs));
	env->CallStaticVoidMethod(classMain, methodMain, args);
	env->DeleteLocalRef(classMain);
	env->DeleteLocalRef(args);
	if (env->ExceptionCheck()) DisplayException(env);
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
		VMInfo info = CreateJVM(dll);
		if (info.result == JNI_OK) {
			RunGame(info.env, lpCmdLine);
			info.vm->DestroyJavaVM();
			FreeLibrary(dll);
			return EXIT_SUCCESS;
		} else {
			std::wstring message = L"Could not create JVM: ";
			message += getErrorString(info.result);
			DisplayError(L"Java Virtual Machine Creation Failed", message.c_str());
			FreeLibrary(dll);
			return EXIT_FAILURE;
		}
	} else {
		DisplayError(L"Java Libray Load Failed", L"Failed to load jvm.dll");
		return EXIT_FAILURE;
	}
}