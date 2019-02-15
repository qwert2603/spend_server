package com.qwert2603.spend_server.repo

import com.qwert2603.spend_server.JDump
import com.qwert2603.spend_server.entity.Dump
import com.qwert2603.spend_server.entity.GetRecordsUpdatesResult
import com.qwert2603.spend_server.entity.Record
import com.qwert2603.spend_server.entity.RecordsCount


interface RecordsRepo {

    /**
     * get user's id by token's hash and update token's expired / last_use fields.
     * @return user_id or null if
     * - token not found
     * - or token in expired
     * - or user is deleted.
     */
    fun getUserId(tokenHash: String): Long?

    /**
     * create new user and token for him.
     * @return token or null if login is used already.
     */
    fun register(login: String, password: String): String?

    /**
     * create new token for user with [login] if [password] is correct and user is not deleted.
     * @return token or null.
     */
    fun login(login: String, password: String): String?

    /**
     * remove token with given [tokenHash].
     */
    fun logout(tokenHash: String)

    /**
     * remove all user's tokens.
     */
    fun logoutAll(userId: Long)

    fun setUserDeleted(userId: Long, deleted: Boolean)

    /**
     * @return list of updated records categories and records updates.
     * both lists are sorted by change_id.
     */
    fun getRecordsUpdates(
            userId: Long,
            lastCategoryChangeId: Long,
            lastRecordChangeId: Long,
            count: Int
    ): GetRecordsUpdatesResult

    /** create or update existing records and update "change_id". */
    fun saveRecords(userId: Long, records: List<Record>)

    /** set "deleted" to true and update "Record.change_id". */
    fun deleteRecords(userId: Long, uuids: List<String>)

    /** just for test. */
    fun getRecordsCount(): RecordsCount

    fun getDump(): Dump

    fun restoreDump(dump: Dump)

    fun restoreJDump(userId: Long,jDump: JDump)

    fun clearAllRecords()
}