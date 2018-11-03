package com.qwert2603.spend_server.repo_impl

import com.qwert2603.spend_entity.LastUpdateInfo
import com.qwert2603.spend_entity.Record
import com.qwert2603.spend_entity.RecordsUpdates
import com.qwert2603.spend_server.db.RemoteDB
import com.qwert2603.spend_server.repo.RecordsRepo

class RecordsRepoImpl(private val remoteDB: RemoteDB) : RecordsRepo {

    override fun getRecordsUpdates(lastUpdate: Long, lastUuid: String, count: Int): RecordsUpdates {
        var lastUpdateInfo = LastUpdateInfo(lastUpdate, lastUuid)
        return remoteDB
                .query(sql = """
                            SELECT
                              uuid,
                              record_type_id,
                              to_char(date, 'YYYYMMDD') AS date,
                              to_char(time, 'HH24MI')   AS time,
                              kind,
                              value,
                              deleted,
                              updated
                            FROM records
                            WHERE updated > ? OR (updated=? AND uuid > ?)
                            ORDER BY updated, uuid
                            LIMIT ?
                            """.trimMargin(),
                        mapper = {
                            if (it.isLast) {
                                lastUpdateInfo = LastUpdateInfo(
                                        lastUpdated = it.getLong("updated"),
                                        lastUuid = it.getString("uuid")
                                )
                            }
                            Record(
                                    uuid = it.getString("uuid"),
                                    recordTypeId = it.getInt("record_type_id"),
                                    date = it.getInt("date").toString(),
                                    time = it.getInt("time").toString(),
                                    kind = it.getString("kind"),
                                    value = it.getInt("value")
                            )
                        },
                        args = listOf(lastUpdate, lastUpdate, lastUuid, count)
                )
                .let { RecordsUpdates(it, lastUpdateInfo) }
    }

    override fun saveRecords(list: List<Record>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteRecords(uuids: List<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRecordsCount(): Int {
        return remoteDB.query("SELECT COUNT (*) FROM records", { it.getInt(1) }).first()
    }
}