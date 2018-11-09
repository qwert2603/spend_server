package com.qwert2603.spend_server.entity

import com.qwert2603.spend_server.utils.SpendServerConst

data class Record(
    val uuid: String,
    val recordTypeId: Long,
    val date: Int, // format is "yyyyMMdd"
    val time: Int?, // format is "HHmm"
    val kind: String,
    val value: Int
) {
    init {
        require(recordTypeId in SpendServerConst.RECORD_TYPE_IDS)
        require(value > 0)
        require(kind.length <= SpendServerConst.MAX_KIND_LENGTH)
    }
}