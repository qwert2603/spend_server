package com.qwert2603.spend_server

import com.qwert2603.spend_server.db.RemoteDBImpl
import com.qwert2603.spend_server.repo.RecordsRepo
import com.qwert2603.spend_server.repo_impl.RecordsRepoImpl
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun Application.module() {

    val recordsRepo: RecordsRepo = RecordsRepoImpl(RemoteDBImpl())

    // todo: status page 404 / 500
    install(ContentNegotiation) {
        jackson {
        }
    }
    routing {
        get("/records_count") {
            call.respond(mapOf("count" to recordsRepo.getRecordsCount()))
        }
        get("/get_records_updates") {
            val receiveParameters = call.request.queryParameters
            call.respond(recordsRepo.getRecordsUpdates(
                    lastUpdate = receiveParameters["last_updated"]?.toLongOrNull()?.takeIf { it >= 0L } ?: 0L,
                    lastUuid = receiveParameters["last_uuid"] ?: "",
                    count = receiveParameters["count"]?.toIntOrNull()?.takeIf { it >= 0 } ?: 10
            ))
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