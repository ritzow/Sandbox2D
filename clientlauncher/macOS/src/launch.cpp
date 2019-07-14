#include "../lib/usr/include/stdio.h"
#include "../lib/usr/include/stdlib.h"
//#include "../lib/usr/include/wchar.h"
#include "../lib/usr/include/dlfcn.h"
//#include "../../Shared/shared.cpp"
#include "../include/jni.h"

JavaVMInitArgs GetJavaInitArgs() {
	static JavaVMOption options[] = {
		{(char*)"-Djdk.module.main=ritzow.sandbox.client"},
		{(char*)"--enable-preview"}, //Java 12 switch expressions
		{(char*)"vfprintf", *vfprintf}
	};

	JavaVMInitArgs args;
	args.version = JNI_VERSION_10;
	args.ignoreUnrecognized = false;
	args.nOptions = sizeof(options) / sizeof(JavaVMOption);
	args.options = options;
	return args;
}

typedef jint(JNICALL* CreateJavaVM)(JavaVM**, JNIEnv**, JavaVMInitArgs*);

int wmain(int argc, const wchar_t* argv[]) {
	void* dylib = dlopen("jdk/Contents/Home/lib/server/libjvm.dylib", RTLD_LAZY | RTLD_LOCAL);
	if (dylib != nullptr) {
		JavaVM* vm; 
		JNIEnv* env;
		jint result = ((CreateJavaVM)dlsym(dylib, "JNI_CreateJavaVM"))
			(&vm, &env, &GetJavaInitArgs());

		if (result == JNI_OK) {
			/*std::wstringstream builder;
			for (int i = 0; i < argc; i++) {
				builder << argv[i];
			}
			RunGame(env, builder.str().c_str());*/

			jclass classMain = env->FindClass("ritzow/sandbox/client/StartClient");
			jmethodID methodMain = env->GetStaticMethodID(classMain, "start", "(Ljava/lang/String;)V");
			jstring jargs = env->NewString((const jchar*)L"", (jsize)0);
			env->CallStaticVoidMethod(classMain, methodMain, jargs);
			env->DeleteLocalRef(classMain);
			env->DeleteLocalRef(jargs);
			if (env->ExceptionCheck()) env->ExceptionDescribe();

			dlclose(dylib);
			return EXIT_SUCCESS;
		} else {
			wprintf(L"Could not create JVM: ");
			//wprintf(L"%ls", getErrorString(result));
			return EXIT_FAILURE;
		}
	} else {
		wprintf(L"failed to load libjvm.dylib");
		return EXIT_FAILURE;
	}
}