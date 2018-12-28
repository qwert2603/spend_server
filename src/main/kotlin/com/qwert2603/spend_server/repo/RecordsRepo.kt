package com.qwert2603.spend_server.repo

import com.qwert2603.spend_server.entity.Dump
import com.qwert2603.spend_server.entity.GetRecordsUpdatesResult
import com.qwert2603.spend_server.entity.Record
import com.qwert2603.spend_server.entity.RecordsCount


interface RecordsRepo {

    /**
     * @return list of updated records categories and records updates.
     * both lists sorted by change_id.
     */
    fun getRecordsUpdates(
            lastCategoryChangeId: Long,
            lastRecordChangeId: Long,
            count: Int
    ): GetRecordsUpdatesResult

    /** create or update existing records and set "updated" to now. */
    fun saveRecords(records: List<Record>)

    /** set "deleted" to true and "Record.updated" to now. */
    fun deleteRecords(uuids: List<String>)

    /** just for test. */
    fun getRecordsCount(): RecordsCount

    fun getDump(): Dump

    fun restoreDump(dump: Dump)

    fun clearAllRecords()
}