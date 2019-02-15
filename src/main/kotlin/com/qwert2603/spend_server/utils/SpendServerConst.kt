package com.qwert2603.spend_server.utils

import com.qwert2603.spend_server.env.E

object SpendServerConst {
    const val TOKEN_EXPIRES_DAYS = 42

    const val MAX_TYPE_LENGTH = 64
    const val MAX_CATEGORY_NAME_LENGTH = 64
    const val MAX_RECORD_KIND_LENGTH = 64

    const val RECORD_TYPE_ID_SPEND = 1L
    const val RECORD_TYPE_ID_PROFIT = 2L

    val RECORD_TYPE_IDS = listOf(
            RECORD_TYPE_ID_SPEND,
            RECORD_TYPE_ID_PROFIT
    )

    const val MAX_ITEMS_TO_SAVE_COUNT = 250
    const val MAX_ITEMS_UPDATES_COUNT = 250

    val DB_URL = "jdbc:postgresql://localhost:5432/${E.env.dbName}"
    const val DB_USER = "postgres"
    const val DB_PASSWORD = "1234"
}