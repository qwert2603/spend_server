package com.qwert2603.spend_server.entity

data class Record(
        val uuid: String,
        val recordTypeId: Int,
        val date: Int,
        val time: Int?,
        val kind: String,
        val value: Int,
        val updated: Long,
        val deleted: Boolean
)