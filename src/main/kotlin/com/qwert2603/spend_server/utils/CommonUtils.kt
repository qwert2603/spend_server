package com.qwert2603.spend_server.utils

import com.qwert2603.spend_server.env.E
import java.net.BindException
import java.net.ServerSocket
import java.security.MessageDigest
import java.sql.ResultSet
import java.util.*

fun ResultSet.getIntNullable(columnLabel: String): Int? = this
        .getInt(columnLabel)
        .takeIf { getObject(columnLabel) != null }

fun Int.toSqlDate(): java.sql.Date {
    val year = this / (100 * 100)
    val month = this / 100 % 100
    val day = this % 100
    return GregorianCalendar.getInstance()
            .also { it.set(year, month - 1, day, 0, 0, 0) }
            .timeInMillis
            .let { java.sql.Date(it) }
}

fun Int.toSqlTime(): java.sql.Time {
    val hour = this / 100
    val minute = this % 100
    return GregorianCalendar.getInstance()
            .also {
                it.set(1970, Calendar.JANUARY, 0, hour, minute, 0)
                it.set(Calendar.MILLISECOND, 0)
            }
            .timeInMillis
            .let { java.sql.Time(it) }
}

fun Long.toSqlTimestamp() = java.sql.Timestamp(this)

fun isServerRunning() = try {
    ServerSocket(E.env.port).close()
    false
} catch (e: BindException) {
    true
}

fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(this.toByteArray(charset("UTF-8")))
    val hexString = StringBuffer()

    for (i in hash.indices) {
        val hex = Integer.toHexString(0xff and hash[i].toInt())
        if (hex.length == 1) hexString.append('0')
        hexString.append(hex)
    }

    return hexString.toString()
}

fun StringBuilder.appendParams(count: Int): StringBuilder {
    append('(')
    repeat(count) { append("?,") }
    this[lastIndex] = ')' // replace ',' to ')'.
    return this
}

fun String.hashWithSalt(): String {
    var result = this
    for (i in 1..256) {
        result = "$result${E.env.salt}".sha256()
    }
    return result
}


@Suppress("UNCHECKED_CAST")
fun <T> List<T>.sortedByMany(selectors: List<(T) -> Any>): List<T> = this
        .sortedWith(Comparator { t1, t2 ->
            for (selector in selectors) {
                val compare = compareBy(selector as ((T) -> Comparable<Any>)).compare(t1, t2)
                if (compare != 0) {
                    return@Comparator compare
                }
            }
            return@Comparator 0
        })