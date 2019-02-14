//package com.qwert2603.spend_server.utils
//
//import com.google.gson.*
//import com.qwert2603.spend_server.entity.*
//import java.io.File
//import java.io.FileWriter
//import java.lang.reflect.Type
//import java.text.SimpleDateFormat
//import java.util.*
//
//object JEntityDeserializer : JsonDeserializer<JEntity> {
//    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): JEntity {
//        val className = json.asJsonObject.get(JEntity::className.name).asString
//        listOf(
//                JUser::class.java,
//                JToken::class.java,
//                JRecordType::class.java,
//                JRecordCategory::class.java,
//                JRecord::class.java
//        ).forEach {
//            if (it.name == className) {
//                return context.deserialize(json, it)
//            }
//        }
//        null!!
//    }
//}
//
//fun main() {
////    val q = listOf(
////            User(1, "klimt"),
////            RecordCategory("uuid1", 1, 1, "c1"),
////            RecordCategory("uuid2", 2, 1, "c1"),
////            Token(2, "tk1"),
////            RecordType(2, "доход"),
////            Record("uuid2", "uuid1", 20190214, 1918, "k1", 12)
////    )
//
//    val gson = GsonBuilder()
//            .registerTypeAdapter(JEntity::class.java, JEntityDeserializer)
//            .create()
//
////    val json = gson.toJson(q)
////    println(json)
////
////    val fromJson = gson.fromJson<List<JEntity>>(json, object : TypeToken<List<JEntity>>() {}.type)
////    fromJson.forEach { println(it) }
////    println(fromJson == q)
//
//
//    val dumpFile = File("/home/alex/spend_dump/qq/dump_2019-02-14_10:11:19")
//
//    val state = State.createEmpty()
//
//    state.addAll(dumpFile.loadEntities(gson))
//
//    println(state)
//
////    FileWriter(dumpFile, true).use { fw ->
////        listOf(
////                User(1, "_klimt"),
////                Token(2, "_tk1"),
////                Record("uuid2", "_uuid1", 20190214, 1918, "k1", 12)
////        ).forEach {
////            fw.write(gson.toJson(it))
////            fw.write("\n")
////        }
////    }
//}
//
//fun File.loadEntities(gson: Gson): List<JEntity> = this
//        .readLines()
//        .map { gson.fromJson<JEntity>(it, JEntity::class.java) }
//
//fun State.addAll(entities: List<JEntity>) {
//    entities.forEach {
//        when (it) {
//            is JUser -> users.put(it.id, it)
//            is JToken -> tokens.put(it.token, it)
//            is JRecordType -> recordTypes.put(it.id, it)
//            is JRecordCategory -> recordCategories.put(it.uuid, it)
//            is JRecord -> records.put(it.uuid, it)
//        }.also { }
//    }
//}