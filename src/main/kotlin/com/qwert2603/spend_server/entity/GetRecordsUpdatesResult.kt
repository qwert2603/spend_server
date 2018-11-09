package com.qwert2603.spend_server.entity

data class GetRecordsUpdatesResult(
    val updatedRecords: List<Record>, // sorted by: updated, uuid
    val deletedRecordsUuid: List<String>,
    val lastUpdateInfo: LastUpdateInfo
)