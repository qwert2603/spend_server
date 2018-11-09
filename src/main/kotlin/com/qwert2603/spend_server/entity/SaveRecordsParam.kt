package com.qwert2603.spend_server.entity

data class SaveRecordsParam(
        val updatedRecords: List<Record>,
        val deletedRecordsUuid: List<String>
)