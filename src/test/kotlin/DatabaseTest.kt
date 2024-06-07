
import com.google.gson.Gson
import gg.ingot.iron.Iron
import gg.ingot.iron.IronSettings
import gg.ingot.iron.annotations.Column
import gg.ingot.iron.serialization.SerializationAdapter
import gg.ingot.iron.sql.allValues
import gg.ingot.iron.sql.executor.prepare
import gg.ingot.iron.sql.executor.prepareMapped
import gg.ingot.iron.sql.executor.queryMapped
import gg.ingot.iron.sql.get
import gg.ingot.iron.sql.singleValue
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

        val results = connection.prepareMapped<TestModel>("SELECT * FROM test")
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

        val result = connection.prepareMapped<TestModel>("SELECT * FROM test").getNext()
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

            connection.prepareMapped<User>("INSERT INTO users VALUES (1), (2) RETURNING *;")
                .single()
        } catch (ex: Exception) {
            assert(ex is IllegalStateException)
        }
    }

    @Test
    fun `gson obj deserialization`() = runTest {
        val ironSerializationInstance = Iron(
            "jdbc:sqlite::memory:",
            IronSettings(
                serialization = SerializationAdapter.Gson(Gson())
            )
        ).connect()

        data class EmbeddedJson(val test: String)
        data class ExampleResponse(
            @Column(json = true)
            val test: EmbeddedJson
        )

        val res = ironSerializationInstance.transaction {
            execute("CREATE TABLE example(id INTEGER PRIMARY KEY, test JSONB)")
            execute("INSERT INTO example(test) VALUES ('{\"test\": \"hello\"}')")
            queryMapped<ExampleResponse>("SELECT * FROM example LIMIT 1;")
                .singleNullable()
        }

        assertNotNull(res)
        assertEquals("hello", res.test.test)
    }

    @Test
    fun `kotlinx obj deserialization`() = runTest {
        val ironSerializationInstance = Iron(
            "jdbc:sqlite::memory:",
            IronSettings(
                serialization = SerializationAdapter.Kotlinx(Json)
            )
        ).connect()

        @Serializable
        data class EmbeddedJson(val test: String)
        data class ExampleResponse(
            @Column(json = true)
            val test: EmbeddedJson
        )

        val res = ironSerializationInstance.transaction {
            execute("CREATE TABLE example(id INTEGER PRIMARY KEY, test JSONB)")
            execute("INSERT INTO example(test) VALUES ('{\"test\": \"hello\"}')")
            queryMapped<ExampleResponse>("SELECT * FROM example LIMIT 1;")
                .singleNullable()
        }

        assertNotNull(res)
        assertEquals("hello", res.test.test)
    }

    @Test
    fun `retrieve single value`() = runTest {
        val name = connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT)")
            execute("INSERT INTO test(name) VALUES ('test1')")

            query("SELECT name FROM test LIMIT 1;")
                .singleValue<String>()
        }

        assertEquals("test1", name)
    }

    @Test
    fun `retrieve list of values`() = runTest {
        val names = connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT)")
            repeat(5) {
                execute("INSERT INTO test(name) VALUES ('test${it}')")
            }

            query("SELECT name FROM test;")
                .allValues<String>()
        }

        assertEquals(5, names.size)
    }

    @Test
    fun `retrieve single value fail`() = runTest {
        try {
            connection.transaction {
                execute("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT)")
                execute("INSERT INTO test(name) VALUES ('test1')")

                query("SELECT * FROM test LIMIT 1;")
                    .singleValue<String>()
           }
        } catch(ex: Exception) {
            assert(ex is IllegalStateException)
        }
    }

    @Test
    fun `retrieve column value by name`() = runTest {
        val name = connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT)")
            execute("INSERT INTO test(name) VALUES ('test1')")

            query("SELECT name FROM test LIMIT 1;")
                .get<String>("name")
        }

        assertEquals("test1", name)
    }
}