
import com.google.gson.Gson
import gg.ingot.iron.Iron
import gg.ingot.iron.IronSettings
import gg.ingot.iron.annotations.Column
import gg.ingot.iron.representation.ExplodingModel
import gg.ingot.iron.serialization.ColumnTransformer
import gg.ingot.iron.serialization.SerializationAdapter
import gg.ingot.iron.sql.sqlParams
import gg.ingot.iron.strategies.NamingStrategy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.sql.SQLException
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatabaseTest {
    private val connection = Iron("jdbc:sqlite::memory:", IronSettings(
        namingStrategy = NamingStrategy.CAMEL_CASE
    )).connect()

    @Test
    fun testIronUse() = runTest {
        val success = connection.use {
            it.createStatement().execute("SELECT 1 + 1")
        }

        assert(success)
    }

    @Test
    fun testIronQuery() = runTest {
        val result = connection.query("SELECT 1 + 1").get<Int>(1)
        assertEquals(2, result)
    }

    @Test
    fun testIronTransaction() = runTest {
        connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY)")
            execute("INSERT INTO test VALUES (1)")
        }

        val result = connection.query("SELECT * FROM test").get<Int>(1)
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

        assertEquals(1, result.get<Int>(1))
        assertEquals("test", result.get<String>(2))
    }

    private data class TestModel(val id: Int, val name: String)
    private class TestModel2 {
        var id: Int = 0
        var name: String = ""
    }

    @Test
    fun testMapperAll() = runTest {
        val results = connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT)")
            execute("INSERT INTO test VALUES (1, 'test1')")
            execute("INSERT INTO test VALUES (2, 'test2')")
            execute("INSERT INTO test VALUES (3, 'test3')")
            execute("INSERT INTO test VALUES (4, 'test4')")
            execute("INSERT INTO test VALUES (5, 'test5')")
            execute("INSERT INTO test VALUES (6, 'test6')")

            prepare("SELECT * FROM test")
                .all<TestModel>()
        }

        results.forEachIndexed { index, result ->
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

        val result = connection.prepare("SELECT * FROM test").getNext<TestModel>()
        assertEquals(1, result?.id)
        assertEquals("test1", result?.name)
    }

    @Test
    fun `get single`() = runTest {
        data class User(val id: Int)

        val user = connection.transaction {
            execute("CREATE TABLE users (id INTEGER PRIMARY KEY)")

            prepare("INSERT INTO users VALUES (1) RETURNING *;")
                .single<User>()
        }

        assertEquals(1, user.id)
    }

    @Test
    fun `fail get single`() = runTest {
        data class User(val id: Int)

        try {
            connection.execute("CREATE TABLE users (id INTEGER PRIMARY KEY)")

            connection.prepare("INSERT INTO users VALUES (1), (2) RETURNING *;")
                .single<User>()
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
            query("SELECT * FROM example LIMIT 1;")
                .singleNullable<ExampleResponse>()
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
            query("SELECT * FROM example LIMIT 1;")
                .singleNullable<ExampleResponse>()
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
                .columnSingle<String>()
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
                .columnAll<String>()
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
                    .columnSingle<String>()
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

    @Test
    fun `exploding model`() = runTest {
        data class TestModel(
            val firstName: String = "Ingot",
            val lastName: String = "Team"
        ) : ExplodingModel

        val model = TestModel()

        val result = connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY, firstName TEXT, lastName TEXT);")
            prepare("INSERT INTO test(firstName, lastName) VALUES (?, ?);", *model.explode())

            prepare("""
                SELECT firstName, lastName FROM test 
                  WHERE firstName = ? AND lastName = ? LIMIT 2;
            """.trimIndent(), *model.explode())
                .single<TestModel>()
        }

        assertEquals("Ingot", result.firstName)
        assertEquals("Team", result.lastName)
    }

    @Test
    fun `improper param size`() = runTest {
        data class TestModel(val a: String = "", val b: String = "") : ExplodingModel
        val model = TestModel()

        try {
            connection.transaction {
                execute("CREATE TABLE test (id INTEGER PRIMARY KEY, a TEXT);")
                prepare("INSERT INTO test(a) VALUES (?);", *model.explode())
            }
        } catch(ex: Exception) {
            assert(ex is IllegalArgumentException)
        }
    }

    @Test
    fun `named placeholder`() = runTest {
        val out = connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT, other_name TEXT);")
            prepare("INSERT INTO test(name, other_name) VALUES (:name, :name);", sqlParams(
                "name" to "test",
            ))

            query("SELECT name, other_name FROM test;")
        }
        out.next()

        connection.prepare("SELECT name FROM test WHERE name = :name;", sqlParams("name" to "test"))
            .columnSingle<String>()
            .let { assertEquals("test", it) }

        assertEquals("test", out.get<String>("name"))
        assertEquals("test", out.get<String>("other_name"))
    }

    @Test
    fun `model named placeholder`() = runTest {
        data class TestModel(val name: String) : ExplodingModel
        val model = TestModel("test")

        val res = connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
            prepare("INSERT INTO test(name) VALUES (:name);", model.toSqlParams())

            query("SELECT name FROM test;")
        }

        res.next()
        assertEquals("test", res.get<String>("name"))
    }

    @Test
    fun `rollback test`() = runTest {
        var rolledBack = false

        try {
            connection.transaction {
                afterRollback {
                    rolledBack = true
                }

                execute("SELECT * FROM fake_table;")
            }
        } catch(ex: Exception) {
            assert(ex is SQLException)
        }

        assertTrue(rolledBack)
    }

    object UUIDTransformer: ColumnTransformer<String, UUID> {
        override fun fromDatabaseValue(value: String): UUID {
            return UUID.fromString(value)
        }

        override fun toDatabaseValue(value: UUID): String {
            return value.toString()
        }
    }

    @Test
    fun `test transformer`() = runTest {
        val values = connection.transaction {
            execute("CREATE TABLE uuids(uuid TEXT PRIMARY KEY)")
            prepare("INSERT INTO uuids VALUES (?)", UUID.randomUUID())
            prepare("INSERT INTO uuids VALUES (?)", UUID.randomUUID())
            prepare("INSERT INTO uuids VALUES (?)", UUID.randomUUID())
            prepare("INSERT INTO uuids VALUES (?)", UUID.randomUUID())

            prepare("SELECT * FROM uuids")
                .columnAll<UUID>(UUIDTransformer)
        }

        assertEquals(4, values.size)
    }

}