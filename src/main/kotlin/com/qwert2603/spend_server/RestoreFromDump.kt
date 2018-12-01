package com.qwert2603.spend_server

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qwert2603.spend_server.db.RemoteDBImpl
import com.qwert2603.spend_server.entity.Dump
import com.qwert2603.spend_server.entity.Record
import com.qwert2603.spend_server.repo.RecordsRepo
import com.qwert2603.spend_server.repo_impl.RecordsRepoImpl
import com.qwert2603.spend_server.utils.SpendServerConst
import java.io.FileReader

data class JRecord(
        val uuid: String,
        val recordTypeId: Long,
        val date: Int,
        val time: Int?,
        val kind: String,
        val value: Int
) {
    fun toRecord() = Record(
            uuid = uuid,
            recordCategoryUuid = when (recordTypeId) {
                SpendServerConst.RECORD_TYPE_ID_SPEND -> "88e52dd4-49a6-8124-ee85-e29047fcd391"
                SpendServerConst.RECORD_TYPE_ID_PROFIT -> "2b74a6d2-7b8f-7fd0-1c05-8002b28cbc7f"
                else -> null!!
            },
            date = date,
            time = time,
            kind = kind,
            value = value
    )
}

fun _main() {
    val filename = "/home/alex/spend_dump/dumps/2018-12-01 18:30:01.json"
    val fileReader = FileReader(filename)
    val json = fileReader.use { it.readText() }
    val jRecords: List<JRecord> = Gson().fromJson(json, object : TypeToken<List<JRecord>>() {}.type)

    val recordsRepo: RecordsRepo = RecordsRepoImpl(RemoteDBImpl())
    recordsRepo.saveRecords(jRecords.map { it.toRecord() })
}


fun main() {
    val filename = "/home/alex/spend_dump/test/dumps/2018-12-01 19:03:47.json"
    val fileReader = FileReader(filename)
    val json = fileReader.use { it.readText() }
    val dump = Gson().fromJson(json, Dump::class.java)

    val recordsRepo: RecordsRepo = RecordsRepoImpl(RemoteDBImpl())
    recordsRepo.restoreDump(dump)
}