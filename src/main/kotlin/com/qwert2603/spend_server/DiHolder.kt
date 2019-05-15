package com.qwert2603.spend_server

import com.qwert2603.spend_server.db.RemoteDBImpl
import com.qwert2603.spend_server.repo.RecordsRepo
import com.qwert2603.spend_server.repo_impl.RecordsRepoImpl

object DiHolder {
    var recordsRepo: RecordsRepo = RecordsRepoImpl(RemoteDBImpl())
}