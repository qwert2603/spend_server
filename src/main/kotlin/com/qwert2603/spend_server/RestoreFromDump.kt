package com.qwert2603.spend_server

import com.google.gson.Gson
import com.qwert2603.spend_server.db.RemoteDBImpl
import com.qwert2603.spend_server.entity.Dump
import com.qwert2603.spend_server.env.E
import com.qwert2603.spend_server.repo.RecordsRepo
import com.qwert2603.spend_server.repo_impl.RecordsRepoImpl
import com.qwert2603.spend_server.utils.LogUtils
import java.io.FileReader
import java.net.BindException
import java.net.ServerSocket

//data class JRecord(
//        val uuid: String,
//        val recordTypeId: Long,
//        val date: Int,
//        val time: Int?,
//        val kind: String,
//        val value: Int
//) {
//    fun toRecord() = Record(
//            uuid = uuid,
//            recordCategoryUuid = when (recordTypeId) {
//                SpendServerConst.RECORD_TYPE_ID_SPEND -> "eac8c137-3025-598a-525f-25cfdda1b7fe"
//                SpendServerConst.RECORD_TYPE_ID_PROFIT -> "145695ca-b5c5-535d-18cc-a8a06998423f"
//                else -> null!!
//            },
//            date = date,
//            time = time,
//            kind = kind,
//            value = value
//    )
//}

//fun main() {
//    val filename = "/home/alex/spend_dump/dumps/2018-12-01 18:30:01.json"
//    val fileReader = FileReader(filename)
//    val json = fileReader.use { it.readText() }
//    val jRecords: List<JRecord> = Gson().fromJson(json, object : TypeToken<List<JRecord>>() {}.type)
//
//    val recordsRepo: RecordsRepo = RecordsRepoImpl(RemoteDBImpl())
//    recordsRepo.saveRecords(jRecords.map { it.toRecord() })
//}

private fun isServerRunning() = try {
    ServerSocket(E.env.port).close()
    false
} catch (e: BindException) {
    true
}

fun main() {
    if (isServerRunning()) {
        LogUtils.e("server is running!")
        return
    }

    val filename = "/file"// "/home/alex/spend_dump/dumps/2018-12-01 19:18:46.json"
    val fileReader = FileReader(filename)
    val json = fileReader.use { it.readText() }
    val dump = Gson().fromJson(json, Dump::class.java)
    LogUtils.d("${dump.categories.size} ${dump.records.size}")

    val recordsRepo: RecordsRepo = RecordsRepoImpl(RemoteDBImpl())
    recordsRepo.restoreDump(dump)
}