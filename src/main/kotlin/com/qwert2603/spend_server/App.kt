package com.qwert2603.spend_server

import com.qwert2603.spend_server.db.RemoteDBImpl
import com.qwert2603.spend_server.entity.PostI
import com.qwert2603.spend_server.entity.Record
import com.qwert2603.spend_server.repo.IntRepo
import com.qwert2603.spend_server.repo.RecordsRepo
import com.qwert2603.spend_server.repo_impl.IntRepoImpl
import com.qwert2603.spend_server.repo_impl.RecordsRepoImpl
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.request.receive
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun Application.module() {

    val intRepo: IntRepo = IntRepoImpl()
    val recordsRepo: RecordsRepo = RecordsRepoImpl(RemoteDBImpl())

    install(ContentNegotiation) {
        jackson {
        }
    }
    routing {
        get("/records") {
            call.respond(listOf(
                    Record("uuid1", 1, 20181102, 1918, "fish", 14, System.currentTimeMillis(), false)
            ))
        }
        route("i") {
            get {
                call.respond(mapOf("i" to intRepo.getI()))
            }
            post {
                intRepo.addI(call.receive<PostI>().add)
                call.respondText { "done" }
            }
        }
        get("records_count") {
            call.respond(mapOf("count" to recordsRepo.getRecordsCount()))
        }
        get("records_updates") {
            call.respond(recordsRepo.getRecordsUpdates(0, "", 100))
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