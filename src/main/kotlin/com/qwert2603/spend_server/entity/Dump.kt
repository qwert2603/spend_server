package com.qwert2603.spend_server.entity

import com.qwert2603.spend_server.utils.sha256

data class Dump(
        val categories: List<RecordCategoryDump>,
        val records: List<RecordDump>
) {
    val hash = toString().sha256()
}

data class RecordDump(
        val uuid: String,
        val recordCategoryUuid: String,
        val date: Int, // format is "yyyyMMdd"
        val time: Int?, // format is "HHmm"
        val kind: String,
        val value: Int,
        val changeId: Long,
        val deleted: Boolean
)

data class RecordCategoryDump(
        val uuid: String,
        val recordTypeId: Long,
        val name: String,
        val changeId: Long
)