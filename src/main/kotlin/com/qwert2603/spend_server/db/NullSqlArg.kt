package com.qwert2603.spend_server.db

/**
 * for use in args parameter in [RemoteDB.query] and [RemoteDB.execute].
 * type is from [java.sql.Types].
 */
data class NullSqlArg(val type: Int)

fun <T> T?.asNullableArg(type: Int): Any {
    if (this != null) {
        return this
    } else {
        return NullSqlArg(type)
    }
}