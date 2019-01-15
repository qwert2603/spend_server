package com.qwert2603.spend_server.entity

data class HashesDump(
        val hash: String,
        val notDeletedRecordsHash: String
)