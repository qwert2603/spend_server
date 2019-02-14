//package com.qwert2603.spend_server.entity
//
//sealed class JEntity {
//    @Suppress("UNUSED")
//    val className: String = this.javaClass.name
//}
//
//data class JUser(
//        val id: Long,
//        val login: String
//) : JEntity()
//
//data class JToken(
//        val userId: Long,
//        val token: String
//) : JEntity()
//
//data class JRecordType(
//        val id: Long,
//        val name: String
//) : JEntity()
//
//data class JRecordCategory(
//        val uuid: String,
//        val recordTypeId: Long,
//        val userId: Long,
//        val name: String
//) : JEntity()
//
//data class JRecord(
//        val uuid: String,
//        val recordCategoryUuid: String,
//        val date: Int, // format is "yyyyMMdd"
//        val time: Int?, // format is "HHmm"
//        val kind: String,
//        val value: Int
//) : JEntity()
//
//
//data class State(
//        val users: HashMap<Long, JUser>,
//        val tokens: HashMap<String, JToken>,
//        val recordTypes: HashMap<Long, JRecordType>,
//        val recordCategories: HashMap<String, JRecordCategory>,
//        val records: HashMap<String, JRecord>
//) {
//    companion object {
//        fun createEmpty() = State(HashMap(), HashMap(), HashMap(), HashMap(), HashMap())
//    }
//}