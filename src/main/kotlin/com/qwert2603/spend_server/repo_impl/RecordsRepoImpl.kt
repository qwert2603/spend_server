package com.qwert2603.spend_server.repo_impl

import com.qwert2603.spend_server.db.RemoteDB
import com.qwert2603.spend_server.db.asNullableArg
import com.qwert2603.spend_server.entity.*
import com.qwert2603.spend_server.repo.RecordsRepo
import com.qwert2603.spend_server.utils.*
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
                lastChangeInfo = LastChangeInfo(
                        lastCategoryChangeId = newLastCategoryChangeId,
                        lastRecordChangeId = newLastRecordChangeId
                )
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
                change_id            = EXCLUDED.change_id
        """.trimIndent())
        remoteDB.execute(
                sql = sb.toString(),
                args = records
                        .map {
                            listOf(
                                    it.uuid,
                                    it.recordCategoryUuid,
                                    it.date.toSqlDate(),
                                    it.time?.toSqlTime().asNullableArg(Types.TIME),
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
    override fun getRecordsCount(): RecordsCount {
        return RecordsCount(
                records = remoteDB.query("SELECT COUNT (*) FROM records WHERE NOT DELETED", { it.getInt(1) }).first(),
                deleted = remoteDB.query("SELECT COUNT (*) FROM records WHERE DELETED", { it.getInt(1) }).first()
        )
    }

    @Synchronized
    override fun getDump(): Dump {
        val records = remoteDB.query(sql = """
                        SELECT
                          uuid,
                          record_category_uuid,
                          to_char(date, 'YYYYMMDD') AS date,
                          to_char(time, 'HH24MI')   AS time,
                          kind,
                          value,
                          change_id,
                          deleted
                        FROM records
                        ORDER BY date, time NULLS FIRST, record_category_uuid, kind, uuid
                        """.trimMargin(),
                mapper = {
                    RecordDump(
                            uuid = it.getString("uuid"),
                            recordCategoryUuid = it.getString("record_category_uuid"),
                            date = it.getInt("date"),
                            time = it.getIntNullable("time"),
                            kind = it.getString("kind"),
                            value = it.getInt("value"),
                            changeId = it.getLong("change_id"),
                            deleted = it.getBoolean("deleted")
                    )
                })

        val categories = remoteDB
                .query(sql = """
                        SELECT
                          uuid,
                          name,
                          record_type_id,
                          change_id
                        FROM record_categories
                        ORDER BY record_type_id, uuid
                        """.trimMargin(),
                        mapper = {
                            RecordCategoryDump(
                                    uuid = it.getString("uuid"),
                                    recordTypeId = it.getLong("record_type_id"),
                                    name = it.getString("name"),
                                    changeId = it.getLong("change_id")
                            )
                        }
                )

        return Dump(categories, records)
    }

    @Synchronized
    override fun restoreDump(dump: Dump) {
        remoteDB.execute("DELETE FROM records")
        remoteDB.execute("DELETE FROM record_categories")

        if (dump.categories.isNotEmpty()) {
            val sb = StringBuilder("INSERT INTO record_categories (uuid, name, record_type_id, change_id) VALUES ")
            repeat(dump.categories.size) { sb.append("(?, ?, ?, ?),") }
            sb.deleteCharAt(sb.lastIndex) // remove last ','.
            remoteDB.execute(
                    sql = sb.toString(),
                    args = dump.categories
                            .map {
                                listOf(
                                        it.uuid,
                                        it.name,
                                        it.recordTypeId,
                                        it.changeId
                                )
                            }
                            .flatten()
            )
        }

        if (dump.records.isNotEmpty()) {
            dump.records.chunked(1000).forEach { chunk ->
                val sb = StringBuilder("INSERT INTO records (uuid, record_category_uuid, date, time, kind, value, change_id, deleted) VALUES ")
                repeat(chunk.size) { sb.append("(?, ?, ?, ?, ?, ?, ?, ?),") }
                sb.deleteCharAt(sb.lastIndex) // remove last ','.
                remoteDB.execute(
                        sql = sb.toString(),
                        args = chunk
                                .map {
                                    listOf(
                                            it.uuid,
                                            it.recordCategoryUuid,
                                            it.date.toSqlDate(),
                                            it.time?.toSqlTime().asNullableArg(Types.TIME),
                                            it.kind,
                                            it.value,
                                            it.changeId,
                                            it.deleted
                                    )
                                }
                                .flatten()
                )
            }
        }

        remoteDB.execute("ALTER SEQUENCE record_types_id_seq RESTART WITH ${SpendServerConst.RECORD_TYPE_IDS.size + 1};")

        val maxCategoryChangeId = dump.categories
                .map { it.changeId }
                .max()
                ?.let { it + 1 }
                ?: 1
        remoteDB.execute("ALTER SEQUENCE category_change_id_seq RESTART WITH $maxCategoryChangeId;")

        val maxRecordChangeId = dump.records
                .map { it.changeId }
                .max()
                ?.let { it + 1 }
                ?: 1
        remoteDB.execute("ALTER SEQUENCE record_change_id_seq RESTART WITH $maxRecordChangeId;")
    }

    @Synchronized
    override fun clearAllRecords() {
        LogUtils.d("RecordsRepoImpl clearAllRecords")
        remoteDB.execute("DELETE FROM records")
    }
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