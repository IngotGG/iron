
import gg.ingot.iron.Iron
import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.controller.Controller
import gg.ingot.iron.controller.tables.controller
import gg.ingot.iron.strategies.NamingStrategy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.asserter

class SimpleTest {
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
        val user = User("User 1", 18, "")

        controller.insert(user)
        user.age = 25

        controller.update(user)
        assertEquals(25, user.age)
    }

}