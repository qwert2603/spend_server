package com.qwert2603.spend_server.repo_impl

import com.qwert2603.spend_server.db.RemoteDB
import com.qwert2603.spend_server.utils.appendParams

class RecordsDbHelper(private val remoteDB: RemoteDB) {

    fun getUsersRecordsUuids(userId: Long, recordsUuids: List<String>): List<String> = remoteDB.query(
            sql = StringBuilder("""
                        select r.uuid
                        from records r
                               left join record_categories c on r.record_category_uuid = c.uuid
                        where c.user_id = ?
                          and r.uuid in
                     """)
                    .also { sb -> sb.appendParams(recordsUuids.size) }
                    .toString(),
            mapper = { it.getString(1) },
            args = listOf(userId) + recordsUuids
    )
}