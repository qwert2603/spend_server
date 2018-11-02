package com.qwert2603.spend_server.repo_impl

import com.qwert2603.spend_server.db.RemoteDB
import com.qwert2603.spend_server.entity.Record
import com.qwert2603.spend_server.entity.RecordPost
import com.qwert2603.spend_server.repo.RecordsRepo

class RecordsRepoImpl(private val remoteDB: RemoteDB) : RecordsRepo {

    override fun getRecordsUpdates(updateMillis: Long, uuid: String, count: Int): List<Record> {
        return remoteDB.query(
                "SELECT * FROM records WHERE updated > ? OR (updated=? AND uuid > ?) LIMIT ?",
                {
                    Record(
                            uuid = it.getString("uuid"),
                            recordTypeId = it.getInt("record_type_id"),
                            date = 12,//todo it.getInt("date"),
                            time = 14,//todo it.getInt("time"),
                            kind = it.getString("kind"),
                            value = it.getInt("value"),
                            updated = it.getLong("updated"),
                            deleted = it.getBoolean("deleted")
                    )
                },
                listOf(updateMillis, updateMillis, uuid, count)
        )
    }

    override fun saveRecords(list: RecordPost) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteRecords(uuids: List<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRecordsCount(): Int {
        return remoteDB.query("SELECT COUNT (*) FROM records", { it.getInt(1) }).first()
    }
}