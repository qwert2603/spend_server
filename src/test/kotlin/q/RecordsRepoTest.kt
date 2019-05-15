package q

import com.qwert2603.spend_server.db.RemoteDBImpl
import com.qwert2603.spend_server.entity.*
import com.qwert2603.spend_server.env.E
import com.qwert2603.spend_server.repo.RecordsRepo
import com.qwert2603.spend_server.repo_impl.RecordsRepoImpl
import com.qwert2603.spend_server.utils.hashWithSalt
import com.qwert2603.spend_server.utils.sortedByMany
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordsRepoTest {

    companion object {
        private val NOW = System.currentTimeMillis()
        private val Int.seconds get() = this * 1000

        private var nextCategoryChangeId = 1L
        private var nextRecordChangeId = 1L

        private fun randomUuid(): String = UUID.randomUUID().toString()

        private fun randomWord(): String = (6..Random.nextInt(10, 12))
                .map { 'a' + Random.nextInt(26) }
                .let { String(it.toCharArray()) }

        private fun randomUser(): UserDump {
            return UserDump(
                    id = Random.nextLong(1, 1_000_000_000),
                    login = randomWord(),
                    deleted = Random.nextBoolean(),
                    passwordHash = randomUuid().hashWithSalt()
            )
        }

        private fun randomToken(userIds: List<Long>): TokenDump {
            return TokenDump(
                    userId = userIds.random(),
                    tokenHash = randomUuid().hashWithSalt(),
                    expires = NOW + 200000.seconds,
                    lastUse = NOW
            )
        }

        private fun randomCategory(userIds: List<Long>): RecordCategoryDump {
            return RecordCategoryDump(
                    uuid = randomUuid(),
                    userId = userIds.random(),
                    recordTypeId = Random.nextLong(1, 3),
                    name = randomWord(),
                    changeId = nextCategoryChangeId++
            )
        }

        private fun randomRecord(categoriesUuids: List<String>): RecordDump {
            val millis = NOW - Random.nextLong(1000L * 60 * 60 * 24 * 1461)
            val calendar = Calendar.getInstance().also { it.timeInMillis = millis }
            return RecordDump(
                    uuid = randomUuid(),
                    recordCategoryUuid = categoriesUuids.random(),
                    date = calendar[Calendar.YEAR] * 10000 + (calendar[Calendar.MONTH] + 1) * 100 + calendar[Calendar.DAY_OF_MONTH],
                    time = if (Random.nextBoolean()) calendar[Calendar.HOUR_OF_DAY] * 100 + calendar[Calendar.MINUTE] else null,
                    kind = randomWord(),
                    value = Random.nextInt(1, 10000),
                    changeId = nextRecordChangeId++,
                    deleted = Random.nextBoolean()
            )
        }

        fun randomDump(): Dump {
            val users = (0 until 20)
                    .map { randomUser() }
                    .sortedBy { it.id }

            val tokens = (0 until 30)
                    .map { randomToken(users.map { it.id }) }
                    .sortedByMany(listOf<(TokenDump) -> Any>(
                            { it.userId },
                            { it.tokenHash }
                    ))

            val categories = (0 until 50)
                    .map { randomCategory(users.map { it.id }) }
                    .sortedByMany(listOf<(RecordCategoryDump) -> Any>(
                            { it.userId },
                            { it.recordTypeId },
                            { it.uuid }
                    ))

            val records = (0 until 200)
                    .map { randomRecord(categories.map { it.uuid }) }
                    .sortedByMany(listOf<(RecordDump) -> Any>(
                            { it.date },
                            { it.time ?: -1 },
                            { it.recordCategoryUuid },
                            { it.kind },
                            { it.uuid }
                    ))

            return Dump(categories, records, users, tokens)
        }
    }

    @Before
    fun checkTest() {
        assertTrue(E.env.forTesting)
    }

    @Test
    fun restoreDump() {
        val dump = randomDump()

        val recordsRepo: RecordsRepo = RecordsRepoImpl(RemoteDBImpl())

        recordsRepo.restoreDump(dump)

        assertEquals(dump, recordsRepo.getDump())
    }
}