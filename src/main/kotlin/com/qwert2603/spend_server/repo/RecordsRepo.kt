package com.qwert2603.spend_server.repo

import com.qwert2603.spend_server.entity.Record
import com.qwert2603.spend_server.entity.RecordPost

interface RecordsRepo {

    /**
     *@return list of records updates where [Record.updated] > [updateMillis] or ([Record.updated]==[updateMillis] and [Record.uuid] > [uuid]).
     * sorted by:
     * - [Record.updated]
     * - [Record.uuid]
     *
     *
     * //todo: this is sort for list in android app.
     * - [Record.date] DESC
     * - [Record.time] DESC NULLS LAST
     * - [Record.recordTypeId]
     * - [Record.kind] DESC
     * - [Record.uuid]
     */
    fun getRecordsUpdates(updateMillis: Long, uuid: String, count: Int): List<Record>

    /** create or update existing records and [Record.updated] to now. */
    fun saveRecords(list: RecordPost)

    /** set [Record.deleted] to true and [Record.updated] to now. */
    fun deleteRecords(uuids: List<String>)

    fun getRecordsCount(): Int
}