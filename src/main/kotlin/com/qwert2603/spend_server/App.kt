package com.qwert2603.spend_server

import com.fasterxml.jackson.core.JsonProcessingException
import com.qwert2603.spend_server.db.RemoteDBImpl
import com.qwert2603.spend_server.entity.BriefInfo
import com.qwert2603.spend_server.entity.SaveRecordsParam
import com.qwert2603.spend_server.env.E
import com.qwert2603.spend_server.repo.RecordsRepo
import com.qwert2603.spend_server.repo_impl.RecordsRepoImpl
import com.qwert2603.spend_server.utils.LogUtils
import com.qwert2603.spend_server.utils.NoTokenException
import com.qwert2603.spend_server.utils.SpendServerConst
import com.qwert2603.spend_server.utils.UserNotFoundException
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun Route.api_v2_0() {
    val recordsRepo: RecordsRepo = RecordsRepoImpl(RemoteDBImpl())

    fun ApplicationCall.getUserId(): Long = this
            .request
            .let { it.queryParameters["token"] ?: throw NoTokenException() }
            .let { recordsRepo.getUserId(it) ?: throw UserNotFoundException() }

    get("get_500") { throw Exception("500 done!") }
    get("get_401") { call.respond(HttpStatusCode.Unauthorized) }

    get("brief_info") {
        call.respond(BriefInfo(
                recordsCount = recordsRepo.getRecordsCount(),
                hashesDump = recordsRepo.getDump().hashes
        ))
    }

    get("dump") {
        call.respond(recordsRepo.getDump())
    }

    get("clear_all_records") {
        if (!E.env.forTesting) {
            call.respond(HttpStatusCode.Forbidden)
            return@get
        }
        recordsRepo.clearAllRecords()
        call.respond(mapOf("result" to "done"))
    }

    get("get_records_updates") {
        val receiveParameters = call.request.queryParameters
        call.respond(recordsRepo.getRecordsUpdates(
                userId = call.getUserId(),
                lastCategoryChangeId = receiveParameters["last_category_change_id"]
                        ?.toLongOrNull()
                        ?.takeIf { it >= 0L }
                        ?: 0L,
                lastRecordChangeId = receiveParameters["last_record_change_id"]
                        ?.toLongOrNull()
                        ?.takeIf { it >= 0L }
                        ?: 0L,
                count = receiveParameters["count"]
                        ?.toIntOrNull()
                        ?.coerceIn(0..SpendServerConst.MAX_ITEMS_UPDATES_COUNT)
                        ?: 10
        ))
    }
    post("save_records") {
        val (updatedRecords, deletedRecordsUuid) = call.receive<SaveRecordsParam>()
        if (updatedRecords.size + deletedRecordsUuid.size > SpendServerConst.MAX_ITEMS_TO_SAVE_COUNT) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }
        recordsRepo.saveRecords(updatedRecords)
        recordsRepo.deleteRecords(deletedRecordsUuid)
        call.respond(mapOf("result" to "done"))
    }

    get("user_id") {
        call.respond(mapOf("user_id" to call.getUserId()))
    }
}

fun Application.module() {

    install(StatusPages) {
        exception<NoTokenException> { call.respond(HttpStatusCode.BadRequest, "no token") }
        exception<UserNotFoundException> { call.respond(HttpStatusCode.Unauthorized, "unauthorized") }
        exception<JsonProcessingException> { cause ->
            LogUtils.e(cause)
            call.respond(HttpStatusCode.BadRequest)
        }
        exception<Throwable> { cause ->
            LogUtils.e(cause)
            call.respond(HttpStatusCode.InternalServerError)
        }
        status(HttpStatusCode.NotFound) {
            call.respond(HttpStatusCode.NotFound, "not found")
        }
    }
    install(ContentNegotiation) {
        jackson {}
    }
    routing {
        route("api/v2.0/", Route::api_v2_0)
    }
}

fun main() {
    embeddedServer(
            factory = Netty,
            port = E.env.port,
            module = Application::module
    ).start()
}