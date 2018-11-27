package com.qwert2603.spend_server.repo_impl

import com.qwert2603.spend_server.db.RemoteDB
import com.qwert2603.spend_server.db.asNullableArg
import com.qwert2603.spend_server.entity.GetRecordsUpdatesResult
import com.qwert2603.spend_server.entity.Record
import com.qwert2603.spend_server.entity.RecordCategory
import com.qwert2603.spend_server.entity.RecordDump
import com.qwert2603.spend_server.repo.RecordsRepo
import com.qwert2603.spend_server.utils.getIntNullable
import com.qwert2603.spend_server.utils.toSqlDate
import com.qwert2603.spend_server.utils.toSqlTime
import java.sql.Types

class RecordsRepoImpl(private val remoteDB: RemoteDB) : RecordsRepo {

    private sealed class RecordChange {
        data class Updated(val record: Record) : RecordChange()
        data class Deleted(val uuid: String) : RecordChange()
    }

    @Synchronized
    override fun getRecordsUpdates(
            lastCategoryChangeId: Long,
            lastRecordChangeId: Long,
            count: Int
    ): GetRecordsUpdatesResult {
        var newLastCategoryChangeId = lastCategoryChangeId
        var newLastRecordChangeId = lastRecordChangeId

        val updatedCategories = remoteDB
                .query(
                        sql = """
                            SELECT
                              uuid,
                              name,
                              record_type_id,
                              change_id
                            FROM record_categories
                            WHERE change_id > ?
                            ORDER BY change_id
                            LIMIT ?
                        """.trimIndent(),
                        mapper = {
                            if (it.isLast) {
                                newLastCategoryChangeId = it.getLong("change_id")
                            }
                            RecordCategory(
                                    uuid = it.getString("uuid"),
                                    recordTypeId = it.getLong("record_type_id"),
                                    name = it.getString("name")
                            )
                        },
                        args = listOf(lastCategoryChangeId, count)
                )

        val recordsChanges = remoteDB
                .query(sql = """
                            SELECT
                              uuid,
                              record_category_uuid,
                              to_char(date, 'YYYYMMDD') AS date,
                              to_char(time, 'HH24MI')   AS time,
                              kind,
                              value,
                              deleted,
                              change_id
                            FROM records
                            WHERE change_id > ?
                            ORDER BY change_id
                            LIMIT ?
                            """.trimMargin(),
                        mapper = {
                            if (it.isLast) {
                                newLastRecordChangeId = it.getLong("change_id")
                            }
                            if (it.getBoolean("deleted")) {
                                RecordChange.Deleted(it.getString("uuid"))
                            } else {
                                RecordChange.Updated(Record(
                                        uuid = it.getString("uuid"),
                                        recordCategoryUuid = it.getString("record_category_uuid"),
                                        date = it.getInt("date"),
                                        time = it.getIntNullable("time"),
                                        kind = it.getString("kind"),
                                        value = it.getInt("value")
                                ))
                            }
                        },
                        args = listOf(lastRecordChangeId, count - updatedCategories.size)
                )

        return GetRecordsUpdatesResult(
                updatedCategories = updatedCategories,
                updatedRecords = recordsChanges.mapNotNull { (it as? RecordChange.Updated)?.record },
                deletedRecordsUuid = recordsChanges.mapNotNull { (it as? RecordChange.Deleted)?.uuid },
                lastCategoryChangeId = newLastCategoryChangeId,
                lastRecordChangeId = newLastRecordChangeId
        )
    }

    @Synchronized
    override fun saveRecords(records: List<Record>) {
        if (records.isEmpty()) return

        val sb = StringBuilder("INSERT INTO records (uuid, record_category_uuid, date, time, kind, value, change_id) VALUES ")
        repeat(records.size) { sb.append("(?, ?, ?, ?, ?, ?, DEFAULT),") }
        sb.deleteCharAt(sb.lastIndex) // remove last ','.
        sb.append("""
            ON CONFLICT (uuid) DO UPDATE SET
                record_category_uuid = EXCLUDED.record_category_uuid,
                date                 = EXCLUDED.date,
                time                 = EXCLUDED.time,
                kind                 = EXCLUDED.kind,
                value                = EXCLUDED.value,
                change_id            = DEFAULT
        """.trimIndent())
        remoteDB.execute(
                sql = sb.toString(),
                args = records
                        .map {
                            listOf(
                                    it.uuid,
                                    it.recordCategoryUuid,
                                    it.date.toSqlDate(),
                                    it.time?.toSqlTime().asNullableArg(Types.TIME_WITH_TIMEZONE),
                                    it.kind,
                                    it.value
                            )
                        }
                        .flatten()
        )
    }

    @Synchronized
    override fun deleteRecords(uuids: List<String>) {
        if (uuids.isEmpty()) return

        val sb = StringBuilder("""
            UPDATE records
            SET deleted = TRUE, change_id = DEFAULT
            WHERE uuid IN (
            """)
        repeat(uuids.size) { sb.append("?,") }
        sb[sb.lastIndex] = ')' // replace ',' to ')'.
        remoteDB.execute(sql = sb.toString(), args = uuids)
    }

    @Synchronized
    override fun getRecordsCount(): Int {
        return remoteDB.query("SELECT COUNT (*) FROM records WHERE NOT DELETED", { it.getInt(1) }).first()
    }

    @Synchronized
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

//    private val database = Database.connect(
//            driver = org.postgresql.Driver::class.java.name,
//            url = SpendServerConst.DB_URL,
//            user = SpendServerConst.DB_USER,
//            password = SpendServerConst.DB_PASSWORD
//    )
//
//    object RecordTypeTable : LongIdTable() {
//        override val id = super.id
//        val name = varchar("name", 64)
//    }
//
//    class RecordType(id: EntityID<Long>) : LongEntity(id) {
//        companion object : LongEntityClass<RecordType>(RecordTypeTable)
//
//        val name by RecordTypeTable.name
//    }
//
//    object RecordCategoryTable : UUIDTable() {
//        override val id = super.id
//        val recordType = reference("recordType", RecordTypeTable)
//        val name = varchar("name", 64)
//    }
//
//    object RecordTable : UUIDTable() {
//        override val id = super.id
//        val recordCategoryUuid = reference("recordCategory", RecordCategoryTable)
//        val date = date("date")
//        val time = datetime("time").nullable()
//        val kind = varchar("kind", 64)
//        val value = integer("value")
//        val updated = long("updated")
//        val deleted = bool("deleted")
//    }
//
//
//    init {
////        transaction {
//
//            SchemaUtils.createMissingTablesAndColumns(RecordTypeTable, RecordCategoryTable, RecordTable)
////        }
//    }