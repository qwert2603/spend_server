package com.qwert2603.spend_server.db

import com.qwert2603.spend_server.utils.LogUtils
import com.qwert2603.spend_server.utils.SpendServerConst
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*

class RemoteDBImpl : RemoteDB {

    init {
        Class.forName(org.postgresql.Driver::class.java.name)
    }

    override fun <T> query(sql: String, mapper: (resultSet: ResultSet) -> T, args: List<Any>): List<T> {
        val uuid = UUID.randomUUID()
        return sendRequest(uuid) {
            LogUtils.d("RemoteDBImpl", "$uuid -->> $sql $args")
            getPreparedStatement(sql)
                    .also { it.setArgs(args) }
                    .executeQuery()
                    .let {
                        val list = mutableListOf<T>()
                        while (it.next()) list.add(mapper(it))
                        list
                    }
                    .also { LogUtils.d("RemoteDBImpl", "$uuid <<-- $sql $args $it") }
        }
    }

    override fun execute(sql: String, args: List<Any>) {
        val uuid = UUID.randomUUID()
        sendRequest(uuid) {
            LogUtils.d("RemoteDBImpl", "$uuid -->> $sql $args")
            getPreparedStatement(sql)
                    .also { it.setArgs(args) }
                    .execute()
            LogUtils.d("RemoteDBImpl", "$uuid <<-- $sql $args")
        }
    }

    private fun getPreparedStatement(sql: String): PreparedStatement = DriverManager
            .getConnection(
                    SpendServerConst.DB_URL,
                    SpendServerConst.DB_USER,
                    SpendServerConst.DB_PASSWORD
            )
            .prepareStatement(sql)

    private fun <T> sendRequest(uuid: UUID, request: () -> T): T {
        try {
            return request()
        } catch (e: Exception) {
            LogUtils.d("RemoteDBImpl", "$uuid <<-- error ${e.message}")
            throw e
        }
    }

    private fun PreparedStatement.setArgs(args: List<Any>) {
        args.forEachIndexed { i, any ->
            val index = i + 1
            when (any) {
                is NullSqlArg -> setNull(index, any.type)
                is Int -> setInt(index, any)
                is Long -> setLong(index, any)
                is Double -> setDouble(index, any)
                is String -> setString(index, any)
                is java.sql.Date -> setDate(index, any)
                is java.sql.Time -> setTime(index, any)
                is Boolean -> setBoolean(index, any)
                else -> LogUtils.e("RemoteDBImpl unknown arg ${any.javaClass} $any")
            }
        }
    }
}