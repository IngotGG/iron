
import gg.ingot.iron.Iron
import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.controller.Controller
import gg.ingot.iron.controller.controller.controller
import gg.ingot.iron.strategies.NamingStrategy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.asserter

class ControllerTest {
    private val iron = Iron("jdbc:sqlite::memory:") {
        namingStrategy = NamingStrategy.SNAKE_CASE
    }.connect()

    @Model
    @Controller
    data class User(
        @Column(primaryKey = true)
        val name: String,
        var age: Int,
        val email: String?,
    )

    @Test
    fun `test controller`() = runTest {
        iron.prepare("CREATE TABLE users (name TEXT, age INTEGER, email TEXT)")
        val controller = iron.controller<User>()

        for (i in 0 until 10) {
            val user = User("User $i", i + 18, "user${i + 1}@example.com")
            controller.insert(user)
        }

        val users = controller.all()
        assertEquals(10, users.size)
        assertEquals(10, controller.count())

        controller.drop()
        try {
            controller.all()
            asserter.fail("Table should have been dropped")
        } catch (e: Exception) {
            // Expected
            assert(true)
        }
    }

    @Test
    fun `test filter`() = runTest {
        iron.prepare("CREATE TABLE users (name TEXT, age INTEGER, email TEXT)")
        val controller = iron.controller<User>()

        for (i in 0 until 10) {
            val user = User("User $i", i + 18, "user${i + 1}@example.com")
            controller.insert(user)
        }

        val user = controller.first {
            (User::age eq 25) and (User::name eq "User 7")
        }

        assertEquals("User 7", user?.name)
        assertEquals(25, user?.age)
    }

    @Test
    fun `test deletion`() = runTest {
        iron.prepare("CREATE TABLE users (name TEXT, age INTEGER, email TEXT)")
        val controller = iron.controller<User>()

        for (i in 0 until 10) {
            val user = User("User $i", i + 18, "")
            controller.insert(user)
        }

        controller.delete {
            (User::age eq 18) or (User::age eq 19)
        }

        assertEquals(8, controller.count())

        controller.clear()

        assertEquals(0, controller.count())
    }

    @Test
    fun `test updating entities`() = runTest {
        iron.prepare("CREATE TABLE users (name TEXT, age INTEGER, email TEXT)")
        val controller = iron.controller<User>()
        var user = User("User 1", 18, "")

        controller.insert(user)
        user.age = 25

        controller.update(user)
        user = controller.first()!!

        assertEquals(25, user.age)
    }

    @Test
    fun `test retrieving all entities with filter`() = runTest {
        iron.prepare("CREATE TABLE users (name TEXT, age INTEGER, email TEXT)")
        val controller = iron.controller<User>()

        for (i in 0 until 10) {
            val user = User("User $i", i + 18, "")
            controller.insert(user)
        }

        val users = controller.all {
            (User::age gt 20) and (User::age lt 25)
        }

        assertEquals(4, users.size)
    }

    @Test
    fun `test interceptors`() = runTest {
        iron.prepare("CREATE TABLE users (name TEXT, age INTEGER, email TEXT)")
        val controller = iron.controller<User>()
        var user = User("User 1", 18, "")

        controller.interceptor {
            it.apply { it.age += 10 }
        }

        user = controller.insert(user, true)
        assertEquals(28, user.age)

        user.age = 30
        user = controller.update(user, true)

        assertEquals(40, user.age)
    }

    @Test
    fun `test inserting many entities`() = runTest {
        iron.prepare("CREATE TABLE users (name TEXT, age INTEGER, email TEXT)")
        val controller = iron.controller<User>()
        var users = (0 until 10).map { User("User $it", it + 18, "") }

        users = controller.insertMany(users)
        assertEquals(10, controller.count())
        assertEquals(10, users.size)
    }

    @Test
    fun `test inserting many entities then fetching`() = runTest {
        iron.prepare("CREATE TABLE users (name TEXT, age INTEGER, email TEXT)")
        val controller = iron.controller<User>()
        var users = (0 until 10).map { User("User $it", it + 18, "") }

        controller.interceptor {
            it.apply { it.age += 10 }
        }

        users = controller.insertMany(users, true)
        assertEquals(10, controller.count())
        assertEquals(10, users.size)

        for (i in 0 until 10) {
            assertEquals(28 + i, users[i].age)
        }
    }

    @Test
    fun `test inserting with reserved keyword`() = runTest {
        @Model
        @Controller
        class Table {
            @Column(primaryKey = true)
            val id: String = "name"
            val default: Boolean = true
        }

        iron.prepare("CREATE TABLE tables (id TEXT PRIMARY KEY, `default` INTEGER)")
        val controller = iron.controller<Table>()
        controller.insert(Table())

        val table = controller.first()
        assertEquals("name", table?.id)
        assertEquals(true, table?.default)
    }

}