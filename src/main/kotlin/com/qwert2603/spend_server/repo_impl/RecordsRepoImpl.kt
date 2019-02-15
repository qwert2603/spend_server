package com.qwert2603.spend_server.repo_impl

import com.qwert2603.spend_server.JDump
import com.qwert2603.spend_server.db.RemoteDB
import com.qwert2603.spend_server.db.asNullableArg
import com.qwert2603.spend_server.entity.*
import com.qwert2603.spend_server.repo.RecordsRepo
import com.qwert2603.spend_server.utils.*
import java.sql.Types
import java.util.*

class RecordsRepoImpl(private val remoteDB: RemoteDB) : RecordsRepo {

    companion object {
        private const val SQL_NOW = "now() at time zone 'utc'"
    }

    private val recordsDbHelper = RecordsDbHelper(remoteDB)

    private fun createNewToken(): String = UUID.randomUUID().toString()

    @Synchronized
    override fun getUserId(tokenHash: String): Long? {
        val userId = remoteDB
                .query(
                        sql = """
                            SELECT u.id
                            FROM tokens t
                                   left join users u on t.user_id = u.id
                            WHERE token_hash = ?
                              and expires > $SQL_NOW
                              and u.deleted = FALSE
                            LIMIT 1;
                        """.trimIndent(),
                        mapper = { it.getLong(1) },
                        args = listOf(tokenHash)
                )
                .firstOrNull()

        if (userId != null) {
            remoteDB.execute(
                    sql = """
                        update tokens
                        set expires = $SQL_NOW + interval '${SpendServerConst.TOKEN_EXPIRES_DAYS} days',
                            last_use = $SQL_NOW
                        where token_hash = ?
                    """.trimIndent(),
                    args = listOf(tokenHash)
            )
        } else {
            // remove expired token.
            remoteDB.execute(
                    sql = "delete from tokens where token_hash = ?",
                    args = listOf(tokenHash)
            )
        }

        return userId
    }

    @Synchronized
    override fun register(login: String, password: String): String? {
        val loginIsUsed = remoteDB
                .query(
                        sql = "select 1 from users where login = ? limit 1",
                        mapper = { Unit },
                        args = listOf(login)
                )
                .isNotEmpty()

        if (loginIsUsed) {
            return null
        }

        remoteDB.execute(
                sql = """
                    insert into users (login, password_hash)
                    values (?, ?)
                """.trimIndent(),
                args = listOf(login, password.hashWithSalt())
        )

        return login(login, password)
    }

    @Synchronized
    override fun login(login: String, password: String): String? {

        val userId: Long = remoteDB
                .query(
                        sql = """
                            select id
                            from users
                            where login = ?
                              and password_hash = ?
                              and deleted = FALSE
                            limit 1
                        """.trimIndent(),
                        mapper = { it.getLong(1) },
                        args = listOf(login, password.hashWithSalt())
                )
                .firstOrNull()
                ?: return null

        val token = createNewToken()

        remoteDB.execute(
                sql = """
                    insert into tokens(user_id, token_hash, expires, last_use)
                    VALUES (?, ?, $SQL_NOW + interval '${SpendServerConst.TOKEN_EXPIRES_DAYS} days', $SQL_NOW)
                """.trimIndent(),
                args = listOf(userId, token.hashWithSalt())
        )

        return token
    }

    @Synchronized
    override fun logout(tokenHash: String) {
        remoteDB.execute(
                sql = "DELETE FROM tokens WHERE token_hash = ?",
                args = listOf(tokenHash)
        )
    }

    @Synchronized
    override fun logoutAll(userId: Long) {
        remoteDB.execute(
                sql = "DELETE FROM tokens WHERE user_id = ?",
                args = listOf(userId)
        )
    }

    @Synchronized
    override fun setUserDeleted(userId: Long, deleted: Boolean) {
        remoteDB.execute(
                sql = "update users set deleted = ? where id = ?",
                args = listOf(deleted, userId)
        )
    }

    private sealed class RecordChange {
        data class Updated(val record: Record) : RecordChange()
        data class Deleted(val uuid: String) : RecordChange()
    }

    @Synchronized
    override fun getRecordsUpdates(
            userId: Long,
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
                            WHERE user_id = ? AND change_id > ?
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
                        args = listOf(userId, lastCategoryChangeId, count)
                )

        val recordsChanges = remoteDB
                .query(sql = """
                            SELECT r.uuid,
                                   record_category_uuid,
                                   to_char(date, 'YYYYMMDD') AS date,
                                   to_char(time, 'HH24MI')   AS time,
                                   kind,
                                   value,
                                   deleted,
                                   r.change_id
                            FROM records r
                                   left join record_categories c on r.record_category_uuid = c.uuid
                            WHERE c.user_id = ?
                              AND r.change_id > ?
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
                        args = listOf(userId, lastRecordChangeId, count - updatedCategories.size)
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
    override fun saveRecords(userId: Long, records: List<Record>) {
        if (records.isEmpty()) return

//        LogUtils.d("RecordsRepoImpl saveRecords $userId $records")

        val otherUsersRecordsUuids = recordsDbHelper
                .getOtherUsersRecordsUuids(userId, records.map { it.uuid })
                .toHashSet()

        if (otherUsersRecordsUuids.isNotEmpty()) {
            LogUtils.e("RecordsRepoImpl saveRecords otherUsersRecordsUuids $otherUsersRecordsUuids")
        }

        val categoriesUuidsToSave = records.map { it.recordCategoryUuid }.distinct()

        val correctCategoriesUuidsToSave = recordsDbHelper
                .getUsersCategoriesUuids(userId, categoriesUuidsToSave)
                .toHashSet()

        if (correctCategoriesUuidsToSave.size != categoriesUuidsToSave.size) {
            LogUtils.e("RecordsRepoImpl saveRecords not correct categories to save ${categoriesUuidsToSave - correctCategoriesUuidsToSave}")
        }

        val filteredRecords = records
                .filter {
                    it.uuid !in otherUsersRecordsUuids
                            && it.recordCategoryUuid in correctCategoriesUuidsToSave
                }

        LogUtils.d("RecordsRepoImpl saveRecords filteredRecords $userId $filteredRecords")

        if (filteredRecords.isEmpty()) return

        val sb = StringBuilder("INSERT INTO records (uuid, record_category_uuid, date, time, kind, value, change_id) VALUES ")
        repeat(filteredRecords.size) { sb.append("(?, ?, ?, ?, ?, ?, DEFAULT),") }
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
                args = filteredRecords
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
    override fun deleteRecords(userId: Long, uuids: List<String>) {
        if (uuids.isEmpty()) return

        LogUtils.d("RecordsRepoImpl deleteRecords $userId $uuids")

        val filteredUuids = recordsDbHelper.getUsersRecordsUuids(userId, uuids)

        if (filteredUuids.size != uuids.size) {
            LogUtils.e("RecordsRepoImpl deleteRecords not correct uuids ${uuids - filteredUuids}")
        }

        if (filteredUuids.isEmpty()) return

        remoteDB.execute(
                sql = StringBuilder("""
                            UPDATE records
                            SET deleted = TRUE, change_id = DEFAULT
                            WHERE uuid IN
                        """)
                        .appendParams(filteredUuids.size)
                        .toString(),
                args = filteredUuids
        )
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
                        SELECT uuid,
                               name,
                               user_id,
                               record_type_id,
                               change_id
                        FROM record_categories
                        ORDER BY user_id, record_type_id, uuid
                        """.trimMargin(),
                        mapper = {
                            RecordCategoryDump(
                                    uuid = it.getString("uuid"),
                                    userId = it.getLong("user_id"),
                                    recordTypeId = it.getLong("record_type_id"),
                                    name = it.getString("name"),
                                    changeId = it.getLong("change_id")
                            )
                        }
                )

        val users = remoteDB.query(
                sql = """
                    select id, login, deleted, password_hash
                    from users
                    order by id
                """.trimIndent(),
                mapper = {
                    UserDump(
                            id = it.getLong("id"),
                            login = it.getString("login"),
                            deleted = it.getBoolean("deleted"),
                            passwordHash = it.getString("password_hash")
                    )
                }
        )

        val tokens = remoteDB.query(
                sql = """
                    select user_id, token_hash, expires, last_use
                    from tokens
                    order by user_id, token_hash
                """.trimIndent(),
                mapper = {
                    TokenDump(
                            userId = it.getLong("user_id"),
                            tokenHash = it.getString("token_hash"),
                            expires = it.getTimestamp("expires").time,
                            lastUse = it.getTimestamp("last_use").time
                    )
                }
        )

        return Dump(categories, records, users, tokens)
    }

    @Synchronized
    override fun restoreDump(dump: Dump) {
        LogUtils.d("RecordsRepoImpl restoreDump ${dump.users.size} ${dump.tokens.size} ${dump.categories.size} ${dump.records.size}")

        remoteDB.execute("DELETE FROM records")
        remoteDB.execute("DELETE FROM record_categories")
        remoteDB.execute("DELETE FROM tokens")
        remoteDB.execute("DELETE FROM users")

        if (dump.users.isNotEmpty()) {
            dump.users.chunked(1000).forEach { chunk ->
                val sb = StringBuilder("INSERT INTO users (id, login, deleted, password_hash) VALUES  ")
                repeat(chunk.size) { sb.append("(?, ?, ?, ?),") }
                sb.deleteCharAt(sb.lastIndex) // remove last ','.
                remoteDB.execute(
                        sql = sb.toString(),
                        args = chunk
                                .map { listOf(it.id, it.login, it.deleted, it.passwordHash) }
                                .flatten()
                )
            }
        }

        if (dump.tokens.isNotEmpty()) {
            dump.tokens.chunked(1000).forEach { chunk ->
                val sb = StringBuilder("INSERT INTO tokens (user_id, token_hash, expires, last_use) VALUES  ")
                repeat(chunk.size) { sb.append("(?, ?, ?, ?),") }
                sb.deleteCharAt(sb.lastIndex) // remove last ','.
                remoteDB.execute(
                        sql = sb.toString(),
                        args = chunk
                                .map {
                                    listOf(
                                            it.userId,
                                            it.tokenHash,
                                            it.expires.toSqlTimestamp(),
                                            it.lastUse.toSqlTimestamp()
                                    )
                                }
                                .flatten()
                )
            }
        }

        if (dump.categories.isNotEmpty()) {
            dump.categories.chunked(1000).forEach { chunk ->
                val sb = StringBuilder("INSERT INTO record_categories (uuid, user_id, name, record_type_id, change_id) VALUES ")
                repeat(chunk.size) { sb.append("(?, ?, ?, ?, ?),") }
                sb.deleteCharAt(sb.lastIndex) // remove last ','.
                remoteDB.execute(
                        sql = sb.toString(),
                        args = chunk
                                .map {
                                    listOf(
                                            it.uuid,
                                            it.userId,
                                            it.name,
                                            it.recordTypeId,
                                            it.changeId
                                    )
                                }
                                .flatten()
                )
            }
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
                .let { it.max() ?: 0 }
                .plus(1)
        remoteDB.execute("ALTER SEQUENCE category_change_id_seq RESTART WITH $maxCategoryChangeId;")

        val maxRecordChangeId = dump.records
                .map { it.changeId }
                .let { it.max() ?: 0 }
                .plus(1)
        remoteDB.execute("ALTER SEQUENCE record_change_id_seq RESTART WITH $maxRecordChangeId;")

        val maxUserId = dump.users
                .map { it.id }
                .let { it.max() ?: 0 }
                .plus(1)
        remoteDB.execute("ALTER SEQUENCE users_id_seq RESTART WITH $maxUserId;")
    }

    @Synchronized
    override fun restoreJDump(userId: Long, jDump: JDump) {
        LogUtils.d("RecordsRepoImpl restoreJDump ${jDump.categories.size} ${jDump.records.size}")

        if (jDump.categories.isNotEmpty()) {
            jDump.categories.chunked(1000).forEach { chunk ->
                val sb = StringBuilder("INSERT INTO record_categories (uuid, user_id, name, record_type_id, change_id) VALUES ")
                repeat(chunk.size) { sb.append("(?, ?, ?, ?, DEFAULT),") }
                sb.deleteCharAt(sb.lastIndex) // remove last ','.
                remoteDB.execute(
                        sql = sb.toString(),
                        args = chunk
                                .map {
                                    listOf(
                                            it.uuid,
                                            userId,
                                            it.name,
                                            it.recordTypeId
                                    )
                                }
                                .flatten()
                )
            }
        }

        if (jDump.records.isNotEmpty()) {
            jDump.records.chunked(1000).forEach { chunk ->
                val sb = StringBuilder("INSERT INTO records (uuid, record_category_uuid, date, time, kind, value, change_id, deleted) VALUES ")
                repeat(chunk.size) { sb.append("(?, ?, ?, ?, ?, ?, DEFAULT, ?),") }
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
                                            it.deleted
                                    )
                                }
                                .flatten()
                )
            }
        }
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