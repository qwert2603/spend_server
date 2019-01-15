package com.qwert2603.spend_server.entity

import com.qwert2603.spend_server.utils.sha256

data class Dump(
        val categories: List<RecordCategoryDump>,
        val records: List<RecordDump>
) {
    val hashes = HashesDump(
            hash = toString().sha256(),
            notDeletedRecordsHash = records
                    .filter { !it.deleted }
                    .map { it.toNotDeletedRecord() }
                    .toString()
                    .sha256()
    )
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

data class HashesDump(
        val hash: String,
        val notDeletedRecordsHash: String
)

data class NotDeletedRecord(
        val uuid: String,
        val recordCategoryUuid: String,
        val date: Int,
        val time: Int?,
        val kind: String,
        val value: Int
)

fun RecordDump.toNotDeletedRecord() = NotDeletedRecord(
        uuid = uuid,
        recordCategoryUuid = recordCategoryUuid,
        date = date,
        time = time,
        kind = kind,
        value = value
)