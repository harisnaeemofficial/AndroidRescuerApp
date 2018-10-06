#include <string>
#include <jni.h>
#include <android/log.h>
#define String std::string

String homeIp = "192.168.100.200";

extern "C" JNIEXPORT jstring JNICALL Java_tuev_konstantin_androidrescuer_nativeLib_urlPath(JNIEnv *env, jobject jobj, jboolean home, jobject url) {
    String response = "";
    if (home == JNI_TRUE) {
        response += "http://"+homeIp+"/server/";
    } else {
        response += "https://androidrescuer.cf/server/";
    }
    jfieldID f = env->GetFieldID((env)->FindClass("tuev/konstantin/androidrescuer/Helper$url"), "value", "I");
    int i = env->GetIntField(url, f);
    switch (i) {
        case -1:
            break;
        case 0:
            response += "add";

            response += "usr";
            break;
        case 1:
            response += "get";

            response += "usr";
            break;
        case 2:
            response += "update";

            response += "token";
            break;
        case 3:
            response += "forgot";

            response += "pass";
            break;
        case 4:
            response += "upda";

            response += "loc";
            break;

        case 5:
            response += "get";

            response += "loc";
            break;

        case 6:
            response += "send";

            response += "control";
            break;
        case 7:
            response += "beta";

            response += "testers";
            break;
        default:break;
    }
    return env->NewStringUTF(response.c_str());
}extern "C"
JNIEXPORT jstring JNICALL
Java_tuev_konstantin_androidrescuer_nativeLib_urlPathImage(JNIEnv *env, jobject instance,
                                                           jboolean home, jstring whichImage_) {
    const char *whichImage = env->GetStringUTFChars(whichImage_, 0);

    String response = "";
    if (home == JNI_TRUE) {
        response += "http://"+homeIp+"/app-images/";
    } else {
        response += "https://androidrescuer.cf/app-images/";
    }
    response += whichImage;
    env->ReleaseStringUTFChars(whichImage_, whichImage);
    return env->NewStringUTF(response.c_str());
}