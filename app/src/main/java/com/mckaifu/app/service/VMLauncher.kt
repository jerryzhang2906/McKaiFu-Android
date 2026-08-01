package com.mckaifu.app.service

object VMLauncher {

    init {
        System.loadLibrary("mckaifu_vm")
    }

    external fun launchJVM(args: Array<String>): Int

    external fun launchJvmChild(args: Array<String>, inFd: Int, outFd: Int): Int

    external fun isProcessAlive(pid: Int): Boolean

    external fun killProcess(pid: Int, sig: Int): Int

    external fun dlopen(path: String, global: Boolean): Boolean

    external fun createPipe(): IntArray?

    external fun setStdio(inFd: Int, outFd: Int)

    external fun restoreStdio()

    external fun chdir(path: String): Int

    external fun readFd(fd: Int, buf: ByteArray, off: Int, len: Int): Int

    external fun writeFd(fd: Int, buf: ByteArray, off: Int, len: Int): Int

    external fun closeFd(fd: Int): Int
}
