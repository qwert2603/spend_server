package com.qwert2603.spend_server.utils

object LogUtils {

    fun d(msg: String) {
        d("AASSDD", msg)
    }

    fun d(tag: String = "AASSDD", msg: String) {
        println("$tag $msg")
    }

    fun e(msg: String) {
        e("AASSDD", msg)
    }

    fun e(t: Throwable) {
        t.printStackTrace()
    }

    fun e(tag: String, msg: String) {
        System.err.println("$tag $msg")
    }
}