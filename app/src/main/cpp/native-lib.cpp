#include <jni.h>
#include <string>
#include <sys/socket.h>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_maoandroidc_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++ Qingdao";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_maoandroidc_MainActivity_setSockOptAPN(JNIEnv *env, jobject thiz, jint sock_fd) {

    int len = 16;
    unsigned char apn6[16] = {
            0x06, 0x01, 0x03, 0x0c,
            0x00, 0x52, 0x27, 0x03,
            0x20, 0x12, 0x20, 0x16,
            0x20, 0x19, 0x20, 0x21
    };

    int ret = setsockopt(sock_fd, SOL_IPV6, 54, apn6, len);
    int err = errno;
    return errno;
}
