package com.qwert2603.spend_server.repo

import com.qwert2603.spend_server.entity.GetRecordsUpdatesResult
import com.qwert2603.spend_server.entity.Record


interface RecordsRepo {

    /**
     * @return list of records updates where "updated" > [lastUpdate] or (["updated" == [lastUpdate] and [Record.uuid] > [lastUuid]).
     * sorted by: updated, uuid.
     *
     *
     * //todo: this is sort for list in android app.
     * - [Record.date] DESC
     * - [Record.time] DESC NULLS LAST
     * - [Record.recordTypeId]
     * - [Record.kind] DESC
     * - [Record.uuid]
     */
    fun getRecordsUpdates(lastUpdate: Long, lastUuid: String, count: Int): GetRecordsUpdatesResult

    /** create or update existing records and set "updated" to now. */
    fun saveRecords(records: List<Record>)

    /** set "deleted" to true and "Record.updated" to now. */
    fun deleteRecords(uuids: List<String>)

    /** just for test. */
    fun getRecordsCount(): Int

    /** for dump. */
    fun getAllRecords(): List<Record>
}