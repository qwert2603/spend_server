package com.qwert2603.spend_server.repo

import com.qwert2603.spend_server.entity.Dump
import com.qwert2603.spend_server.entity.GetRecordsUpdatesResult
import com.qwert2603.spend_server.entity.Record
import com.qwert2603.spend_server.entity.RecordsCount


interface RecordsRepo {

    /**
     * @return user_id or null if not found.
     */
    fun getUserId(token: String): Long?

    /**
     * @return list of updated records categories and records updates.
     * both lists are sorted by change_id.
     */
    fun getRecordsUpdates(
            userId: Long,
            lastCategoryChangeId: Long,
            lastRecordChangeId: Long,
            count: Int
    ): GetRecordsUpdatesResult

    /** create or update existing records and update "change_id". */
    fun saveRecords(userId: Long, records: List<Record>)

    /** set "deleted" to true and update "Record.change_id". */
    fun deleteRecords(userId: Long, uuids: List<String>)

    /** just for test. */
    fun getRecordsCount(): RecordsCount

    fun getDump(): Dump

    fun restoreDump(dump: Dump)

    fun clearAllRecords()
}