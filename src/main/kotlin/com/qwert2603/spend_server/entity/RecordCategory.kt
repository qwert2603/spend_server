package com.qwert2603.spend_server.entity

import com.qwert2603.spend_server.utils.SpendServerConst

data class RecordCategory(
        val uuid: String,
        val recordTypeId: Long,
        val name: String
) {
    init {
        require(recordTypeId in SpendServerConst.RECORD_TYPE_IDS)
        require(name.length in 1..SpendServerConst.MAX_CATEGORY_NAME_LENGTH)
    }
}