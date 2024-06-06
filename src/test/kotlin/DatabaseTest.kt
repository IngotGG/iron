
import com.google.gson.Gson
import gg.ingot.iron.Iron
import gg.ingot.iron.IronSettings
import gg.ingot.iron.annotations.Column
import gg.ingot.iron.serialization.ColumnDeserializer
import gg.ingot.iron.serialization.SerializationAdapter
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.assertNotNull

class DatabaseTest {
    private val connection = Iron("jdbc:sqlite::memory:")
        .connect()

    @Test
    fun testIronUse() = runTest {
        val success = connection.use {
            it.createStatement().execute("SELECT 1 + 1")
        }

        assert(success)
    }

    @Test
    fun testIronQuery() = runTest {
        val result = connection.query("SELECT 1 + 1").getInt(1)
        assertEquals(2, result)
    }

    @Test
    fun testIronTransaction() = runTest {
        connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY)")
            execute("INSERT INTO test VALUES (1)")
        }

        val result = connection.query("SELECT * FROM test").getInt(1)
        assertEquals(1, result)
    }

    @Test
    fun testIronBrokenTransaction() = runTest {
        try {
            connection.transaction {
                execute("CREATE TABLE test (id INTEGER PRIMARY KEY)")
                execute("INSERT INTO test VALUES (1)")
                throw IllegalStateException("This is a test")
            }
        } catch (e: IllegalStateException) {
            // expected
        }

        try {
            connection.query("SELECT * FROM test")
        } catch (e: Exception) {
            // expected
            assert(e is SQLException)
        }
    }

    @Test
    fun testIronPrepared() = runTest {
        val result = connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT)")

            prepare(
                "INSERT INTO test VALUES (?, ?)",
                1, "test"
            )

            prepare("SELECT * FROM test")
        }

        assertEquals(1, result?.getInt(1))
        assertEquals("test", result?.getString(2))
    }

    private data class TestModel(val id: Int, val name: String)
    private class TestModel2 {
        var id: Int = 0
        var name: String = ""
    }

    @Test
    fun testMapperAll() = runTest {
        connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT)")
            execute("INSERT INTO test VALUES (1, 'test1')")
            execute("INSERT INTO test VALUES (2, 'test2')")
            execute("INSERT INTO test VALUES (3, 'test3')")
            execute("INSERT INTO test VALUES (4, 'test4')")
            execute("INSERT INTO test VALUES (5, 'test5')")
            execute("INSERT INTO test VALUES (6, 'test6')")
        }

        val results = connection.prepare<TestModel>("SELECT * FROM test")
        results.all().forEachIndexed { index, result ->
            assertEquals(index + 1, result.id)
            assertEquals("test${index + 1}", result.name)
        }
    }

    @Test
    fun testMapperNext() = runTest {
        connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT)")
            execute("INSERT INTO test VALUES (1, 'test1')")
            execute("INSERT INTO test VALUES (2, 'test2')")
            execute("INSERT INTO test VALUES (3, 'test3')")
            execute("INSERT INTO test VALUES (4, 'test4')")
            execute("INSERT INTO test VALUES (5, 'test5')")
            execute("INSERT INTO test VALUES (6, 'test6')")
        }

        val result = connection.prepare<TestModel>("SELECT * FROM test").getNext()
        assertEquals(1, result?.id)
        assertEquals("test1", result?.name)
    }

    @Test
    fun `get single`() = runTest {
        data class User(val id: Int)

        val user = connection.transaction {
            execute("CREATE TABLE users (id INTEGER PRIMARY KEY)")

            prepare<User>("INSERT INTO users VALUES (1) RETURNING *;")
                .single()
        }

        assertEquals(1, user.id)
    }

    @Test
    fun `fail get single`() = runTest {
        data class User(val id: Int)

        try {
            connection.execute("CREATE TABLE users (id INTEGER PRIMARY KEY)")

            connection.prepare<User>("INSERT INTO users VALUES (1), (2) RETURNING *;")
                .single()
        } catch (ex: Exception) {
            assert(ex is IllegalStateException)
        }
    }
}