package q

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.*
import com.qwert2603.spend_server.DiHolder
import com.qwert2603.spend_server.ROUTE_PATH
import com.qwert2603.spend_server.entity.Dump
import com.qwert2603.spend_server.entity.RegisterParams
import com.qwert2603.spend_server.env.E
import com.qwert2603.spend_server.module
import com.qwert2603.spend_server.repo.RecordsRepo
import com.qwert2603.spend_server.utils.hashWithSalt
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppTest {

    @Before
    fun checkTest() {
        assertTrue(E.env.forTesting)
    }

    @Test
    fun getDump() {
        val dump = RecordsRepoTest.randomDump()

        val recordsRepo: RecordsRepo = mock {
            on { getDump() } doReturn dump
            on { getUserId("h1".hashWithSalt()) } doReturn 1
            on { getUserId("h2".hashWithSalt()) } doReturn 2
        }
        DiHolder.recordsRepo = recordsRepo

        withTestApplication(Application::module) {

            with(handleRequest(HttpMethod.Get, "$ROUTE_PATH/dump?token=h1")) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(Gson().fromJson(response.content, Dump::class.java), dump)
            }

            verify(recordsRepo).getUserId("h1".hashWithSalt())
            verify(recordsRepo).getDump()
            verifyNoMoreInteractions(recordsRepo)

            with(handleRequest(HttpMethod.Get, "$ROUTE_PATH/dump?token=h2")) {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }

            verify(recordsRepo).getUserId("h2".hashWithSalt())
            verifyNoMoreInteractions(recordsRepo)
        }
    }

    @Test
    fun `status pages`() {
        val recordsRepo: RecordsRepo = mock()
        DiHolder.recordsRepo = recordsRepo

        withTestApplication(Application::module) {

            with(handleRequest(HttpMethod.Get, "$ROUTE_PATH/get_401")) {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
            with(handleRequest(HttpMethod.Get, "$ROUTE_PATH/get_500")) {
                assertEquals(HttpStatusCode.InternalServerError, response.status())
            }

            verifyZeroInteractions(recordsRepo)
        }
    }

    @Test
    fun getUserId() {
        val recordsRepo: RecordsRepo = mock {
            on { getUserId("h0".hashWithSalt()) } doReturn null
            on { getUserId("h1".hashWithSalt()) } doReturn 14
        }
        DiHolder.recordsRepo = recordsRepo

        withTestApplication(Application::module) {
            with(handleRequest(HttpMethod.Get, "$ROUTE_PATH/user_id")) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }

            verifyZeroInteractions(recordsRepo)

            with(handleRequest(HttpMethod.Get, "$ROUTE_PATH/user_id?token=h0")) {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }

            verify(recordsRepo).getUserId("h0".hashWithSalt())
            verifyNoMoreInteractions(recordsRepo)

            with(handleRequest(HttpMethod.Get, "$ROUTE_PATH/user_id?token=h1")) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("{\"user_id\":14}", response.content)
            }

            verify(recordsRepo).getUserId("h1".hashWithSalt())
            verifyNoMoreInteractions(recordsRepo)
        }
    }

    @Test
    fun register() {
        val recordsRepo: RecordsRepo = mock {
            on { register("u1", "12") } doReturn "h1"
            on { register("u4", "14") } doReturn null
        }
        DiHolder.recordsRepo = recordsRepo

        withTestApplication(Application::module) {
            with(handleRequest(HttpMethod.Post, "$ROUTE_PATH/register") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(RegisterParams("u1", "12").toJson())
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("{\"token\":\"h1\"}", response.content)
            }

            verify(recordsRepo).register("u1", "12")
            verifyNoMoreInteractions(recordsRepo)

            with(handleRequest(HttpMethod.Post, "$ROUTE_PATH/register") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(RegisterParams("u4", "14").toJson())
            }) {
                assertEquals(HttpStatusCode.Conflict, response.status())
            }

            verify(recordsRepo).register("u4", "14")
            verifyNoMoreInteractions(recordsRepo)
        }
    }

    companion object {
        private val gson = Gson()

        private fun <T> T.toJson(): String = gson.toJson(this)
    }
}