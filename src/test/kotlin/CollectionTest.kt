
import gg.ingot.iron.Iron
import gg.ingot.iron.annotations.Model
import kotlinx.coroutines.test.runTest
import java.sql.SQLFeatureNotSupportedException
import kotlin.test.Test

// TODO: Sqlite doesn't support arrays, so we need to figure out how to test this
class CollectionTest {

    enum class Permission {
        A, B, C
    }

    @Model
    data class UserList(
        val id: Int,
        val permissions: List<Permission>
    )

    @Suppress("ArrayInDataClass")
    @Model
    data class UserArray(
        val id: Int,
        val permissions: Array<Permission>
    )

    @Test
    fun `test collection`() = runTest {
        val iron = Iron("jdbc:sqlite::memory:").connect()

        iron.prepare("CREATE TABLE users(id INTEGER PRIMARY KEY, permissions TEXT[]);")
        iron.prepare("INSERT INTO users(id, permissions) VALUES (?, ?);", 1, listOf(Permission.A, Permission.B, Permission.C))

        try {
            iron.prepare("SELECT * FROM users WHERE id = ?;", 1).single<UserList>()
        } catch(ex: SQLFeatureNotSupportedException) {
            // expected
        } catch(ex: Exception) {
            ex.printStackTrace()
            assert(false)
        }
    }

    @Test
    fun `test array`() = runTest {
        val iron = Iron("jdbc:sqlite::memory:").connect()

        iron.prepare("CREATE TABLE users(id INTEGER PRIMARY KEY, permissions TEXT[]);")
        iron.prepare("INSERT INTO users(id, permissions) VALUES (?, ?);", 1, arrayOf(Permission.A, Permission.B, Permission.C))

        try {
            iron.prepare("SELECT * FROM users WHERE id = ?;", 1).single<UserArray>()
        } catch(ex: SQLFeatureNotSupportedException) {
            // expected
        } catch(ex: Exception) {
            ex.printStackTrace()
            assert(false)
        }
    }

    @Test
    fun `test array with model insertion`() = runTest {
        val iron = Iron("jdbc:sqlite::memory:").connect()

        iron.prepare("CREATE TABLE users(id INTEGER PRIMARY KEY, permissions TEXT[]);")

        val user = UserArray(1, arrayOf(Permission.A, Permission.B, Permission.C))
        // TODO: broken
        iron.prepare("INSERT INTO users(id, permissions) VALUES (:id, :permissions);", user)

        try {
            iron.prepare("SELECT * FROM users WHERE id = ?;", 1).single<UserArray>()
        } catch(ex: SQLFeatureNotSupportedException) {
            // expected
        } catch(ex: Exception) {
            ex.printStackTrace()
            assert(false)
        }
    }

    @Test
    fun `test collection with model insertion`() = runTest {
        val iron = Iron("jdbc:sqlite::memory:").connect()

        iron.prepare("CREATE TABLE users(id INTEGER PRIMARY KEY, permissions TEXT[]);")

        val user = UserList(1, listOf(Permission.A, Permission.B, Permission.C))
        iron.prepare("INSERT INTO users(id, permissions) VALUES (:id, :permissions);", user)

        try {
            iron.prepare("SELECT * FROM users WHERE id = ?;", 1).single<UserList>()
        } catch(ex: SQLFeatureNotSupportedException) {
            // expected
        } catch(ex: Exception) {
            ex.printStackTrace()
            assert(false)
        }
    }

}