package com.qwert2603.spend_server

import com.fasterxml.jackson.core.JsonProcessingException
import com.qwert2603.spend_server.db.RemoteDBImpl
import com.qwert2603.spend_server.entity.SaveRecordsParam
import com.qwert2603.spend_server.env.E
import com.qwert2603.spend_server.repo.RecordsRepo
import com.qwert2603.spend_server.repo_impl.RecordsRepoImpl
import com.qwert2603.spend_server.utils.LogUtils
import com.qwert2603.spend_server.utils.SpendServerConst
import com.qwert2603.spend_server.utils.applyRange
import io.ktor.application.Application
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

fun Route.api_v1_0() {
    val recordsRepo: RecordsRepo = RecordsRepoImpl(RemoteDBImpl())

    get("get_500") { throw Exception("500 done!") }
    get("get_401") { call.respond(HttpStatusCode.Unauthorized) }

    get("records_count") {
        call.respond(mapOf("count" to recordsRepo.getRecordsCount()))
    }

    get("dump") {
        call.respond(recordsRepo.getAllRecords())
    }

    get("get_records_updates") {
        val receiveParameters = call.request.queryParameters
        call.respond(recordsRepo.getRecordsUpdates(
                lastUpdate = receiveParameters["last_updated"]
                        ?.toLongOrNull()
                        ?.takeIf { it >= 0L }
                        ?: 0L,
                lastUuid = receiveParameters["last_uuid"] ?: "",
                count = receiveParameters["count"]
                        ?.toIntOrNull()
                        ?.applyRange(0..SpendServerConst.MAX_RECORDS_UPDATES_COUNT)
                        ?: 10
        ))
    }
    post("save_records") {
        val (updatedRecords, deletedRecordsUuid) = call.receive<SaveRecordsParam>()
        if (updatedRecords.size + deletedRecordsUuid.size > SpendServerConst.MAX_RECORDS_TO_SAVE_COUNT) {
            call.respond(HttpStatusCode.BadRequest)
        }
        recordsRepo.saveRecords(updatedRecords)
        recordsRepo.deleteRecords(deletedRecordsUuid)
        call.respond(mapOf("result" to "done"))
    }
}

fun Application.module() {

    install(StatusPages) {
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
        jackson {
        }
    }
    routing {
        route("api/v1.0/", Route::api_v1_0)
    }
}

fun main(args: Array<String>) {
    embeddedServer(
            factory = Netty,
            port = E.env.port,
            module = Application::module
    ).start()
}