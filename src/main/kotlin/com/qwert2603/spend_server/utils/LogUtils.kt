package com.qwert2603.spend_server.utils

import java.text.SimpleDateFormat
import java.util.*

object LogUtils {

    fun d(msg: String) {
        d("AASSDD", msg)
    }

    fun d(tag: String = "AASSDD", msg: String) {
        println("${nowString()} $tag $msg")
    }

    fun e(msg: String) {
        e("AASSDD", msg)
    }

    fun e(t: Throwable) {
        t.printStackTrace()
    }

    fun e(tag: String, msg: String) {
        System.err.println("${nowString()} $tag $msg")
    }

    private fun nowString() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
}