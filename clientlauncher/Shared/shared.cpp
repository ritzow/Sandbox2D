#include <sstream>
#include <jni.h>

static JavaVMOption JVM_OPTIONS[] = {
	{(char*)"--enable-preview"},
	{(char*)"-Djdk.module.main=ritzow.sandbox.client"},
	{(char*)"-XX:-UsePerfData"},
	{(char*)"-Dorg.lwjgl.util.NoChecks=true"},
	{(char*)"-Dorg.lwjgl.openal.explicitInit=true"},
	{(char*)"-Dorg.lwjgl.opengl.explicitInit=true"},
	{(char*)"-Dorg.lwjgl.util.NoFunctionChecks=true"} //TODO set these properties using Configuration class instead of system properties.
};

constexpr JavaVMInitArgs GetJavaInitArgs() noexcept {
	return {
		.version = JNI_VERSION_10,
		.nOptions = sizeof(JVM_OPTIONS) / sizeof(JavaVMOption),
		.options = JVM_OPTIONS,
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
	builder << message << '\n';
	env->ReleaseStringUTFChars(msgString, message);
	env->DeleteLocalRef(msgString);
	jsize length = env->GetArrayLength(trace);
	for (int i = 0; i < length; i++) {
		jobject element = env->GetObjectArrayElement(trace, i);
		jstring msg = static_cast<jstring>(env->CallObjectMethod(element, elementToString));
		const jchar* str = env->GetStringChars(msg, nullptr);
		builder << L"  at " << (const wchar_t*)str << '\n';
		env->ReleaseStringChars(msg, str);
		env->DeleteLocalRef(msg);
	}
	env->DeleteLocalRef(trace);
	return builder.str().c_str();
}

void RunGame(JNIEnv* env, int argc, wchar_t** args) {
	jclass classMain = env->FindClass("ritzow/sandbox/client/StartClient");
	jclass classString = env->FindClass("java/lang/String");
	jmethodID methodMain = env->GetStaticMethodID(classMain, "main", "([Ljava/lang/String;)V");
	jobjectArray jargs = env->NewObjectArray(argc, classString, nullptr);
	env->DeleteLocalRef(classString);
	for (int i = 0; i < argc; i++) {
		jstring jarg = env->NewString((const jchar*)args[i], (jsize)wcslen(args[i]));
		env->SetObjectArrayElement(jargs, i, jarg);
		env->DeleteLocalRef(jarg);
	}
	env->CallStaticVoidMethod(classMain, methodMain, jargs);
	env->DeleteLocalRef(jargs);
	env->DeleteLocalRef(classMain);
	if (env->ExceptionCheck())
		DisplayError(L"Java Exception Occurred on Main Thread", GetExceptionString(env));
}

typedef jint(JNICALL* StartJVM)(JavaVM**, JNIEnv**, JavaVMInitArgs*);