package com.qwert2603.spend_server.utils

import java.sql.ResultSet

fun ResultSet.getIntNullable(columnLabel: String): Int? = this
        .getInt(columnLabel)
        .takeIf { getObject(columnLabel) != null }

fun Int.applyRange(intRange: IntRange) = when {
    this < intRange.first -> intRange.first
    this > intRange.last -> intRange.last
    else -> this
}