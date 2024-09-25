
import gg.ingot.iron.Iron
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.strategies.EnumTransformation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@Suppress("USELESS_IS_CHECK")
class EnumTest {

    enum class Permission {
        A, B, C
    }

    @Model
    data class User(val id: Int, val permission: Permission)

    @Test
    fun `test enum by transforming with names`() = runTest {
        val iron = Iron("jdbc:sqlite::memory:").connect()
        val user = User(1, Permission.B)

        iron.prepare("CREATE TABLE users(id INTEGER PRIMARY KEY, permission TEXT);")
        iron.prepare("INSERT INTO users(id, permission) VALUES (:id, :permission);", user)

        val result = iron.prepare("SELECT * FROM users WHERE id = ?;", user.id).single<User>()
        assert(result.permission == Permission.B)
    }

    @Test
    fun `test enum by transforming with ordinals`() = runTest {
        val iron = Iron("jdbc:sqlite::memory:") {
            enumTransformation = EnumTransformation.Ordinal
        }.connect()

        val user = User(1, Permission.B)

        iron.prepare("CREATE TABLE users(id INTEGER PRIMARY KEY, permission TEXT);")
        iron.prepare("INSERT INTO users(id, permission) VALUES (:id, :permission);", user)

        val result = iron.prepare("SELECT * FROM users WHERE id = ?;", user.id).single<User>()
        assert(result.permission == Permission.B)
    }
}