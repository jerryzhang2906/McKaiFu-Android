#include <jni.h>
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <signal.h>
#include <sys/wait.h>
#include <android/log.h>

#define LOG_TAG "mckaifu-vm"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

typedef jint (*JLI_Launch_func)(int argc, char **argv,
        int jargc, const char **jargv,
        int appclassc, const char **appclassv,
        const char *fullversion, const char *dotversion,
        const char *pname, const char *lname,
        jboolean javaargs, jboolean cpwildcard,
        jboolean javaw, jint ergo);

static int saved_out = -1;
static int saved_in = -1;

/* 由调用方传入已释放的 argv(仅保留到 fork 前) */
static jint runJVM(int argc, char **argv) {
    void *libjli = dlopen("libjli.so", RTLD_NOW | RTLD_GLOBAL);
    if (!libjli) {
        LOGE("dlopen libjli.so failed: %s", dlerror());
        return -1;
    }
    JLI_Launch_func launch = (JLI_Launch_func)dlsym(libjli, "JLI_Launch");
    if (!launch) {
        LOGE("dlsym JLI_Launch failed: %s", dlerror());
        return -2;
    }
    LOGI("Calling JLI_Launch with %d args", argc);
    return launch(argc, argv,
            0, NULL, 0, NULL,
            "17.0.10-internal", "17",
            argv[0] ? argv[0] : "java", "openjdk",
            JNI_FALSE, JNI_TRUE, JNI_FALSE, 0);
}

/* fork 子进程运行 JVM: 子进程崩溃/退出不影响宿主进程 */
JNIEXPORT jint JNICALL Java_com_mckaifu_app_service_VMLauncher_launchJvmChild(
        JNIEnv *env, jclass clazz, jobjectArray argsArray, jint inFd, jint outFd) {
    if (argsArray == NULL) return -3;
    jsize argc = (*env)->GetArrayLength(env, argsArray);
    char **argv = (char **)calloc(argc + 1, sizeof(char *));
    if (!argv) return -4;
    for (jsize i = 0; i < argc; i++) {
        jstring js = (jstring)(*env)->GetObjectArrayElement(env, argsArray, i);
        const char *s = (*env)->GetStringUTFChars(env, js, NULL);
        argv[i] = s ? strdup(s) : NULL;
        (*env)->ReleaseStringUTFChars(env, js, s);
    }
    argv[argc] = NULL;

    fflush(stdout);
    fflush(stderr);

    pid_t pid = fork();
    if (pid < 0) {
        LOGE("fork failed: %s", strerror(errno));
        for (jsize i = 0; i < argc; i++) if (argv[i]) free(argv[i]);
        free(argv);
        return -5;
    }
    if (pid == 0) {
        /* 子进程: 只运行 JVM, 退出时绕过 atexit/全局析构 */
        if (outFd >= 0) { dup2(outFd, 1); dup2(outFd, 2); }
        if (inFd >= 0) dup2(inFd, 0);
        if (outFd > 2) close(outFd);
        if (inFd > 2 && inFd != outFd) close(inFd);
        jint res = runJVM(argc, argv);
        LOGI("JLI_Launch returned %d in child", res);
        _exit(res);
    }
    /* 父进程 */
    for (jsize i = 0; i < argc; i++) if (argv[i]) free(argv[i]);
    free(argv);
    LOGI("forked child pid=%d", pid);
    return pid;
}

JNIEXPORT jboolean JNICALL Java_com_mckaifu_app_service_VMLauncher_isProcessAlive(
        JNIEnv *env, jclass clazz, jint pid) {
    if (pid <= 0) return JNI_FALSE;
    if (kill(pid, 0) == 0) return JNI_TRUE;
    return errno != ESRCH ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_com_mckaifu_app_service_VMLauncher_killProcess(
        JNIEnv *env, jclass clazz, jint pid, jint sig) {
    if (pid <= 0) return -1;
    return kill(pid, sig);
}

JNIEXPORT jint JNICALL Java_com_mckaifu_app_service_VMLauncher_launchJVM(
        JNIEnv *env, jclass clazz, jobjectArray argsArray) {
    if (argsArray == NULL) return -3;
    jsize argc = (*env)->GetArrayLength(env, argsArray);
    char **argv = (char **)calloc(argc + 1, sizeof(char *));
    if (!argv) return -4;
    for (jsize i = 0; i < argc; i++) {
        jstring js = (jstring)(*env)->GetObjectArrayElement(env, argsArray, i);
        const char *s = (*env)->GetStringUTFChars(env, js, NULL);
        argv[i] = s ? strdup(s) : NULL;
        (*env)->ReleaseStringUTFChars(env, js, s);
    }
    argv[argc] = NULL;
    jint res = runJVM(argc, argv);
    for (jsize i = 0; i < argc; i++) if (argv[i]) free(argv[i]);
    free(argv);
    return res;
}

JNIEXPORT jboolean JNICALL Java_com_mckaifu_app_service_VMLauncher_dlopen(
        JNIEnv *env, jclass clazz, jstring path, jboolean global) {
    if (path == NULL) return JNI_FALSE;
    const char *p = (*env)->GetStringUTFChars(env, path, NULL);
    int flags = RTLD_NOW | (global == JNI_TRUE ? RTLD_GLOBAL : RTLD_LOCAL);
    void *h = dlopen(p, flags);
    if (!h) LOGE("dlopen %s failed: %s", p, dlerror());
    (*env)->ReleaseStringUTFChars(env, path, p);
    return h != NULL ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jintArray JNICALL Java_com_mckaifu_app_service_VMLauncher_createPipe(
        JNIEnv *env, jclass clazz) {
    int fds[2] = {-1, -1};
    if (pipe(fds) != 0) return NULL;
    int flags = fcntl(fds[0], F_GETFD);
    if (flags != -1) fcntl(fds[0], F_SETFD, flags & ~FD_CLOEXEC);
    flags = fcntl(fds[1], F_GETFD);
    if (flags != -1) fcntl(fds[1], F_SETFD, flags & ~FD_CLOEXEC);
    jintArray arr = (*env)->NewIntArray(env, 2);
    if (arr == NULL) { close(fds[0]); close(fds[1]); return NULL; }
    jint vals[2] = {fds[0], fds[1]};
    (*env)->SetIntArrayRegion(env, arr, 0, 2, vals);
    return arr;
}

JNIEXPORT jint JNICALL Java_com_mckaifu_app_service_VMLauncher_setStdio(
        JNIEnv *env, jclass clazz, jint inFd, jint outFd) {
    if (saved_out < 0) saved_out = dup(1);
    if (saved_in < 0) saved_in = dup(0);
    if (outFd >= 0) { dup2(outFd, 1); dup2(outFd, 2); }
    if (inFd >= 0) dup2(inFd, 0);
    return 0;
}

JNIEXPORT jint JNICALL Java_com_mckaifu_app_service_VMLauncher_restoreStdio(
        JNIEnv *env, jclass clazz) {
    if (saved_out >= 0) { dup2(saved_out, 1); dup2(saved_out, 2); close(saved_out); saved_out = -1; }
    if (saved_in >= 0) { dup2(saved_in, 0); close(saved_in); saved_in = -1; }
    return 0;
}

JNIEXPORT jint JNICALL Java_com_mckaifu_app_service_VMLauncher_chdir(
        JNIEnv *env, jclass clazz, jstring path) {
    if (path == NULL) return -1;
    const char *p = (*env)->GetStringUTFChars(env, path, NULL);
    int r = chdir(p);
    (*env)->ReleaseStringUTFChars(env, path, p);
    return r;
}

JNIEXPORT jint JNICALL Java_com_mckaifu_app_service_VMLauncher_readFd(
        JNIEnv *env, jclass clazz, jint fd, jbyteArray buf, jint off, jint len) {
    if (buf == NULL) return -1;
    jbyte *data = (*env)->GetByteArrayElements(env, buf, NULL);
    if (!data) return -1;
    ssize_t n = read(fd, data + off, len);
    (*env)->ReleaseByteArrayElements(env, buf, data, JNI_ABORT);
    return (jint)n;
}

JNIEXPORT jint JNICALL Java_com_mckaifu_app_service_VMLauncher_writeFd(
        JNIEnv *env, jclass clazz, jint fd, jbyteArray buf, jint off, jint len) {
    if (buf == NULL) return -1;
    jbyte *data = (*env)->GetByteArrayElements(env, buf, NULL);
    if (!data) return -1;
    ssize_t n = write(fd, data + off, len);
    (*env)->ReleaseByteArrayElements(env, buf, data, JNI_ABORT);
    return (jint)n;
}

JNIEXPORT jint JNICALL Java_com_mckaifu_app_service_VMLauncher_closeFd(
        JNIEnv *env, jclass clazz, jint fd) {
    return close(fd);
}
