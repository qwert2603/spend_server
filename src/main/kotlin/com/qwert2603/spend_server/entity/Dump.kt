package com.qwert2603.spend_server.entity

import com.qwert2603.spend_server.utils.sha256

data class Dump(
        val categories: List<RecordCategoryDump>,
        val records: List<RecordDump>,
        val users: List<UserDump>,
        val tokens: List<TokenDump>
) {
    private val categoriesByUuid: Map<String, RecordCategoryDump> = categories.associateBy { it.uuid }
    private val usersById: Map<Long, UserDump> = users.associateBy { it.id }

    val hashes = HashesDump(
            hash = toString().sha256(),
            notDeletedRecordsHash = records.getNotDeletedRecordsHash(),
            notDeletedRecordsHashByUser = records
                    .groupBy { categoriesByUuid.getValue(it.recordCategoryUuid).userId }
                    .mapKeys { usersById.getValue(it.key).login }
                    .mapValues { it.value.getNotDeletedRecordsHash() }
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
        val userId: Long,
        val recordTypeId: Long,
        val name: String,
        val changeId: Long
)

data class UserDump(
        val id: Long,
        val login: String,
        val deleted: Boolean,
        val passwordHash: String
)

data class TokenDump(
        val userId: Long,
        val tokenHash: String,
        val expires: Long,
        val lastUse: Long
)

data class NotDeletedRecord(
        val uuid: String,
        val recordCategoryUuid: String,
        val date: Int,
        val time: Int?,
        val kind: String,
        val value: Int
)

private fun RecordDump.toNotDeletedRecord() = NotDeletedRecord(
        uuid = uuid,
        recordCategoryUuid = recordCategoryUuid,
        date = date,
        time = time,
        kind = kind,
        value = value
)

private fun List<RecordDump>.getNotDeletedRecordsHash() = this
        .filter { !it.deleted }
        .sortedBy { it.uuid }
        .map { it.toNotDeletedRecord() }
        .toString()
        .sha256()