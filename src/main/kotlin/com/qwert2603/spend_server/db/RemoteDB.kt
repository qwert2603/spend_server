package com.qwert2603.spend_server.db

import java.sql.ResultSet

interface RemoteDB {
    fun <T> query(
            sql: String,
            mapper: (resultSet: ResultSet) -> T,
            args: List<Any> = emptyList()
    ): List<T>

    fun execute(
            sql: String,
            args: List<Any> = emptyList()
    )
}