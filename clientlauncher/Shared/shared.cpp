#ifdef __APPLE__
	#include "../macOS/include/jni.h"
#elif _WIN32
	#include "../Windows/include/jni.h"
	#include <string>
	#include <sstream>
	#include <vector>
#endif

#if 0
void LoadMainModule(JNIEnv* env) {
	jclass classModuleLayer = env->FindClass("java/lang/ModuleLayer");
	jmethodID mBootModule = env->GetStaticMethodID(classModuleLayer,
		"boot", "()java/lang/ModuleLayer;");
	jobject layer = env->CallStaticObjectMethod(classModuleLayer, mBootModule);
}
#endif

JavaVMInitArgs GetJavaInitArgs() noexcept {
	static JavaVMOption options[] = {
		{(char*)"-Djdk.module.main=ritzow.sandbox.client"},
		{(char*)"--enable-preview"}, //switch expressions
		//{(char*)"vfprintf", *vfprintf}
	};

	return {
		.version = JNI_VERSION_10,
		.nOptions = sizeof(options) / sizeof(JavaVMOption),
		.options = options,
		.ignoreUnrecognized = false
	};
}

constexpr const wchar_t* GetErrorString(jint result) noexcept {
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

inline void DisplayError(const wchar_t* title, const wchar_t* message);

const wchar_t* GetExceptionString(JNIEnv* env) noexcept {
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
	return builder.str().c_str();
}

void RunGame(JNIEnv* env, const wchar_t* args) {
	jclass classMain = env->FindClass("ritzow/sandbox/client/StartClient");
	jmethodID methodMain = env->GetStaticMethodID(classMain, "start", "(Ljava/lang/String;)V");
	jstring jargs = env->NewString((const jchar*)args, (jsize)wcslen(args));
	env->CallStaticVoidMethod(classMain, methodMain, jargs);
	env->DeleteLocalRef(classMain);
	env->DeleteLocalRef(jargs);
	if (env->ExceptionCheck())
		DisplayError(L"Java Exception Occurred on Main Thread", GetExceptionString(env));
}

typedef jint(JNICALL* StartJVM)(JavaVM**, JNIEnv**, JavaVMInitArgs*);