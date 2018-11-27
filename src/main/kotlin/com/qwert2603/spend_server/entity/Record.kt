package com.qwert2603.spend_server.entity

import com.qwert2603.spend_server.utils.SpendServerConst

data class Record(
        val uuid: String,
        val recordCategoryUuid: String,
        val date: Int, // format is "yyyyMMdd"
        val time: Int?, // format is "HHmm"
        val kind: String,
        val value: Int
) {
    init {
        require(value > 0)
        require(kind.length in 1..SpendServerConst.MAX_RECORD_KIND_LENGTH)
    }
}