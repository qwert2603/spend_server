package com.qwert2603.spend_server

import com.google.gson.Gson
import com.qwert2603.spend_server.db.RemoteDBImpl
import com.qwert2603.spend_server.entity.RecordDump
import com.qwert2603.spend_server.repo.RecordsRepo
import com.qwert2603.spend_server.repo_impl.RecordsRepoImpl
import com.qwert2603.spend_server.utils.LogUtils
import com.qwert2603.spend_server.utils.isServerRunning
import java.io.FileReader

data class JDump(
        val categories: List<JRecordCategoryDump>,
        val records: List<RecordDump>
)

data class JRecordCategoryDump(
        val uuid: String,
        val recordTypeId: Long,
        val name: String,
        val changeId: Long
)


fun main() {
    if (isServerRunning()) {
        LogUtils.e("server is running!")
        return
    }

    val userId = 0L
    val filename = ""
    val fileReader = FileReader(filename)
    val json = fileReader.use { it.readText() }
    val jDump = Gson().fromJson(json, JDump::class.java)

    val recordsRepo: RecordsRepo = RecordsRepoImpl(RemoteDBImpl())
    recordsRepo.restoreJDump(userId, jDump)

    LogUtils.d("dump's hashes = ${recordsRepo.getDump().hashes}")
}