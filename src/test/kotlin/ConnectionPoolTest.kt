
import gg.ingot.iron.Iron
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class ConnectionPoolTest {

    @Test
    fun `test exhausting connection pool`() = runTest {
        val iron = Iron("jdbc:sqlite::memory:") {
            maximumConnections = 3
            connectionPollTimeout = 3.seconds
        }.connect()

        val jobs = List(10) {
            launch {
                iron.use {
                    it.createStatement().execute("SELECT 1;")
                }
            }
        }

        jobs.joinAll()
        iron.close()
    }

}