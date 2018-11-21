package com.qwert2603.spend_server.entity

data class RecordDump(
        val uuid: String,
        val recordTypeId: Long,
        val date: Int, // format is "yyyyMMdd"
        val time: Int?, // format is "HHmm"
        val kind: String,
        val value: Int,
        val updated: Long,
        val deleted: Boolean
)