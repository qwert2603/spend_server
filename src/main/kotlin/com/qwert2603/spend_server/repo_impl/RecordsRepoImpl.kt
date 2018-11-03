package com.qwert2603.spend_server.repo_impl

import com.qwert2603.spend_entity.LastUpdateInfo
import com.qwert2603.spend_entity.Record
import com.qwert2603.spend_entity.RecordsUpdates
import com.qwert2603.spend_server.db.RemoteDB
import com.qwert2603.spend_server.db.asNullableArg
import com.qwert2603.spend_server.repo.RecordsRepo
import com.qwert2603.spend_server.utils.getIntNullable
import com.qwert2603.spend_server.utils.toSqlDate
import com.qwert2603.spend_server.utils.toSqlTime
import java.sql.Types

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
                                    date = it.getInt("date"),
                                    time = it.getIntNullable("time"),
                                    kind = it.getString("kind"),
                                    value = it.getInt("value")
                            )
                        },
                        args = listOf(lastUpdate, lastUpdate, lastUuid, count)
                )
                .let { RecordsUpdates(it, lastUpdateInfo) }
    }

    override fun saveRecords(records: List<Record>) {
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
}