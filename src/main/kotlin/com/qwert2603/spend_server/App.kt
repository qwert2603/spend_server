package com.qwert2603.spend_server

import com.fasterxml.jackson.core.JsonProcessingException
import com.qwert2603.spend_entity.RecordsDelete
import com.qwert2603.spend_server.db.RemoteDBImpl
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
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun Application.module() {

    val recordsRepo: RecordsRepo = RecordsRepoImpl(RemoteDBImpl())

    install(StatusPages) {
        exception<JsonProcessingException> {
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
        get("get_500") { throw Exception("500 done!") }
        get("get_401") { call.respond(HttpStatusCode.Unauthorized) }

        get("/records_count") {
            call.respond(mapOf("count" to recordsRepo.getRecordsCount()))
        }

        get("/get_records_updates") {
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
        delete("delete_records") {
            val uuids = call.receive<RecordsDelete>().uuids
            if (uuids.isEmpty()) call.respond(HttpStatusCode.NoContent)
            if (uuids.size > SpendServerConst.MAX_RECORDS_TO_DELETE_COUNT) call.respond(HttpStatusCode.BadRequest)
            recordsRepo.deleteRecords(uuids)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

fun main(args: Array<String>) {
    embeddedServer(
            factory = Netty,
            port = 8359,
            module = Application::module
    ).start()
}