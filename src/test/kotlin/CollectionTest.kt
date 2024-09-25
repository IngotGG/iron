
import gg.ingot.iron.Iron
import gg.ingot.iron.annotations.Model
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@Suppress("USELESS_IS_CHECK")
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

        val result = iron.prepare("SELECT * FROM users WHERE id = ?;", 1).single<UserList>()
        assert(result.permissions is List<*>)
        assert(result.permissions.size == 3)
        assert(result.permissions[0] == Permission.A)
        assert(result.permissions[1] == Permission.B)
        assert(result.permissions[2] == Permission.C)
    }

    @Test
    fun `test array`() = runTest {
        val iron = Iron("jdbc:sqlite::memory:").connect()

        iron.prepare("CREATE TABLE users(id INTEGER PRIMARY KEY, permissions TEXT[]);")
        iron.prepare("INSERT INTO users(id, permissions) VALUES (?, ?);", 1, arrayOf(Permission.A, Permission.B, Permission.C))

        val result = iron.prepare("SELECT * FROM users WHERE id = ?;", 1).single<UserArray>()
        assert(result.permissions is Array<*>)
        assert(result.permissions.size == 3)
        assert(result.permissions[0] == Permission.A)
        assert(result.permissions[1] == Permission.B)
        assert(result.permissions[2] == Permission.C)
    }

    @Test
    fun `test array with model insertion`() = runTest {
        val iron = Iron("jdbc:sqlite::memory:").connect()

        iron.prepare("CREATE TABLE users(id INTEGER PRIMARY KEY, permissions TEXT[]);")

        val user = UserArray(1, arrayOf(Permission.A, Permission.B, Permission.C))
        // TODO: broken
        iron.prepare("INSERT INTO users(id, permissions) VALUES (?, ?);", user)

        val result = iron.prepare("SELECT * FROM users WHERE id = ?;", user.id).single<UserArray>()
        assert(result.permissions is Array<*>)
        assert(result.permissions.size == 3)
        assert(result.permissions[0] == Permission.A)
        assert(result.permissions[1] == Permission.B)
        assert(result.permissions[2] == Permission.C)
    }

    @Test
    fun `test collection with model insertion`() = runTest {
        val iron = Iron("jdbc:sqlite::memory:").connect()

        iron.prepare("CREATE TABLE users(id INTEGER PRIMARY KEY, permissions TEXT[]);")

        val user = UserList(1, listOf(Permission.A, Permission.B, Permission.C))
        // TODO: broken
        iron.prepare("INSERT INTO users(id, permissions) VALUES (?, ?);", user)

        val result = iron.prepare("SELECT * FROM users WHERE id = ?;", user.id).single<UserList>()
        assert(result.permissions is List<*>)
        assert(result.permissions.size == 3)
        assert(result.permissions[0] == Permission.A)
        assert(result.permissions[1] == Permission.B)
        assert(result.permissions[2] == Permission.C)
    }

}