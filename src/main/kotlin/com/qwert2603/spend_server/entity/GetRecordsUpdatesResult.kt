package com.qwert2603.spend_server.entity

data class GetRecordsUpdatesResult(
        val updatedCategories: List<RecordCategory>, // sorted by change_id
        val updatedRecords: List<Record>, // sorted by change_id
        val deletedRecordsUuid: List<String>,
        val lastChangeInfo: LastChangeInfo
)