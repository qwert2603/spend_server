package com.qwert2603.spend_server.repo_impl

import com.qwert2603.spend_server.db.RemoteDB
import com.qwert2603.spend_server.db.asNullableArg
import com.qwert2603.spend_server.entity.GetRecordsUpdatesResult
import com.qwert2603.spend_server.entity.LastUpdateInfo
import com.qwert2603.spend_server.entity.Record
import com.qwert2603.spend_server.entity.RecordDump
import com.qwert2603.spend_server.repo.RecordsRepo
import com.qwert2603.spend_server.utils.getIntNullable
import com.qwert2603.spend_server.utils.toSqlDate
import com.qwert2603.spend_server.utils.toSqlTime
import java.sql.ResultSet
import java.sql.Types

class RecordsRepoImpl(private val remoteDB: RemoteDB) : RecordsRepo {

    companion object {
        private fun ResultSet.makeRecord() = Record(
                uuid = getString("uuid"),
                recordTypeId = getLong("record_type_id"),
                date = getInt("date"),
                time = getIntNullable("time"),
                kind = getString("kind"),
                value = getInt("value")
        )
    }

    private sealed class RecordChange {
        data class Updated(val record: Record) : RecordChange()
        data class Deleted(val uuid: String) : RecordChange()
    }

    override fun getRecordsUpdates(lastUpdate: Long, lastUuid: String, count: Int): GetRecordsUpdatesResult {
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
                            if (it.getBoolean("deleted")) {
                                RecordChange.Deleted(it.getString("uuid"))
                            } else {
                                RecordChange.Updated(it.makeRecord())
                            }
                        },
                        args = listOf(lastUpdate, lastUpdate, lastUuid, count)
                )
                .let { changes ->
                    GetRecordsUpdatesResult(
                            updatedRecords = changes.mapNotNull { (it as? RecordChange.Updated)?.record },
                            deletedRecordsUuid = changes.mapNotNull { (it as? RecordChange.Deleted)?.uuid },
                            lastUpdateInfo = lastUpdateInfo
                    )
                }
    }

    override fun saveRecords(records: List<Record>) {
        if (records.isEmpty()) return

        val sb = StringBuilder("INSERT INTO records (uuid, record_type_id, date, time, kind, value, updated) VALUES ")
        repeat(records.size) { sb.append("(?, ?, ?, ?, ?, ?, DEFAULT),") }
        sb.deleteCharAt(sb.lastIndex) // remove last ','.
        sb.append("""
            ON CONFLICT (uuid) DO UPDATE SET
                record_type_id = EXCLUDED.record_type_id,
                date           = EXCLUDED.date,
                time           = EXCLUDED.time,
                kind           = EXCLUDED.kind,
                value          = EXCLUDED.value,
                updated        = DEFAULT
        """.trimIndent())
        remoteDB.execute(
                sql = sb.toString(),
                args = records
                        .map {
                            listOf(
                                    it.uuid,
                                    it.recordTypeId,
                                    it.date.toSqlDate(),
                                    it.time?.toSqlTime().asNullableArg(Types.TIME_WITH_TIMEZONE),
                                    it.kind,
                                    it.value
                            )
                        }
                        .flatten()
        )
    }

    override fun deleteRecords(uuids: List<String>) {
        if (uuids.isEmpty()) return

        val sb = StringBuilder("""
            UPDATE records
            SET deleted = TRUE, updated = DEFAULT
            WHERE uuid IN (
            """)
        repeat(uuids.size) { sb.append("?,") }
        sb[sb.lastIndex] = ')' // replace ',' to ')'.
        remoteDB.execute(sql = sb.toString(), args = uuids)
    }

    override fun getRecordsCount(): Int {
        return remoteDB.query("SELECT COUNT (*) FROM records", { it.getInt(1) }).first()
    }

    override fun getAllRecords(): List<RecordDump> = remoteDB
            .query(sql = """
                        SELECT
                          uuid,
                          record_type_id,
                          to_char(date, 'YYYYMMDD') AS date,
                          to_char(time, 'HH24MI')   AS time,
                          kind,
                          value,
                          updated,
                          deleted
                        FROM records
                        ORDER BY date, time NULLS FIRST, record_type_id, kind, uuid
                        """.trimMargin(),
                    mapper = {
                        RecordDump(
                                uuid = it.getString("uuid"),
                                recordTypeId = it.getLong("record_type_id"),
                                date = it.getInt("date"),
                                time = it.getIntNullable("time"),
                                kind = it.getString("kind"),
                                value = it.getInt("value"),
                                updated = it.getLong("updated"),
                                deleted = it.getBoolean("deleted")
                        )
                    }
            )
}