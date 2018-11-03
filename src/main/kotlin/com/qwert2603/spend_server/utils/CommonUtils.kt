package com.qwert2603.spend_server.utils

import java.sql.ResultSet
import java.util.*

fun ResultSet.getIntNullable(columnLabel: String): Int? = this
        .getInt(columnLabel)
        .takeIf { getObject(columnLabel) != null }

fun Int.applyRange(intRange: IntRange) = when {
    this < intRange.first -> intRange.first
    this > intRange.last -> intRange.last
    else -> this
}

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
            .also { it.set(1970, Calendar.JANUARY, 0, hour, minute, 0) }
            .timeInMillis
            .let { java.sql.Time(it) }
}