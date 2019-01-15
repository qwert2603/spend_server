package com.qwert2603.spend_server

import com.qwert2603.spend_server.db.RemoteDBImpl
import com.qwert2603.spend_server.repo.RecordsRepo
import com.qwert2603.spend_server.repo_impl.RecordsRepoImpl
import com.qwert2603.spend_server.utils.LogUtils
import com.qwert2603.spend_server.utils.isServerRunning

fun main() {
    if (isServerRunning()) {
        LogUtils.e("server is running!")
        return
    }

    val recordsRepo: RecordsRepo = RecordsRepoImpl(RemoteDBImpl())

    LogUtils.d("dump's hashes = ${recordsRepo.getDump().hashes}")
}
