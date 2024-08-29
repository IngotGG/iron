
import com.google.gson.Gson
import gg.ingot.iron.Iron
import gg.ingot.iron.IronSettings
import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.representation.ExplodingModel
import gg.ingot.iron.serialization.ColumnAdapter
import gg.ingot.iron.serialization.SerializationAdapter
import gg.ingot.iron.sql.params.sqlParams
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
    private val connection = Iron("jdbc:sqlite::memory:") {
        namingStrategy = NamingStrategy.SNAKE_CASE
    }.connect()

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

    @Model
    private data class TestModel(val id: Int, val name: String)

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
        @Model
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
        @Model
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

        @Model
        data class ExampleResponse(
            @Column(json = true)
            val test: EmbeddedJson,
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
                .single<String>()
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
                .all<String>()
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
                    .single<String>()
           }
        } catch(ex: Exception) {
            assert(ex is ClassCastException)
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
        @Model
        data class TestModel(
            val firstName: String = "Ingot",
            val lastName: String = "Team"
        ) : ExplodingModel

        val model = TestModel()

        val result = connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY, first_name TEXT, last_name TEXT);")
            prepare("INSERT INTO test(first_name, last_name) VALUES (:firstName, :lastName);", model)

            prepare("""
                SELECT first_name, last_name FROM test 
                  WHERE first_name = :firstName AND last_name = :lastName LIMIT 2;
            """.trimIndent(), model)
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
                prepare("INSERT INTO test(a) VALUES (?);", model)
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
            .single<String>()
            .let { assertEquals("test", it) }

        assertEquals("test", out.get<String>("name"))
        assertEquals("test", out.get<String>("other_name"))
    }

    @Test
    fun `model named placeholder`() = runTest {
        @Model
        data class TestModel(val name: String)
        val model = TestModel("test")

        val res = connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
            prepare("INSERT INTO test(name) VALUES (:name);", model)

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

    object UUIDTransformer: ColumnAdapter<String, UUID> {
        override fun fromDatabaseValue(value: String): UUID {
            return UUID.fromString(value)
        }

        override fun toDatabaseValue(value: UUID): String {
            return value.toString()
        }
    }

    @Test
    fun `test transformer`() = runTest {
        val value = connection.transaction {
            execute("CREATE TABLE uuids(uuid TEXT PRIMARY KEY)")
            repeat(3) {
                prepare("INSERT INTO uuids VALUES (?)", UUID.randomUUID())
            }
            prepare("INSERT INTO uuids VALUES (?) RETURNING *", UUID.randomUUID())
                .single<UUID>(UUIDTransformer)
        }

        assertNotNull(value)
    }

    @Test
    fun `add named params to exploding model`() = runTest {
        @Model
        data class TestModel(val id: String, val hello: String)
        val model = TestModel("a", "b")
        val model2 = TestModel("c", "d")

        @Model
        data class TestTableModel(val id: String, val hello: String, val another: String)

        val models = connection.transaction {
            prepare("CREATE TAbLE test(id TEXT PRIMARY KEY, hello TEXT, another TEXT);")

            prepare(
                "INSERT INTO test(id, hello, another) VALUES (:id, :hello, :another)",
                sqlParams("another" to "c") + sqlParams(model)
            )

            prepare(
                "INSERT INTO test(id, hello, another) VALUES (:id, :hello, :another)",
                sqlParams(model2) + sqlParams("another" to "c")
            )

            query("SELECT * FROM test")
                .all<TestTableModel>()
        }

        assertEquals(2, models.size)
        assertEquals("a", models[0].id)
        assertEquals("c", models[0].another)

        assertEquals("c", models[1].id)
        assertEquals("c", models[1].another)
    }

    @Test
    fun `data class getter support`() = runTest {
        @Model
        data class TestModel(val id: String) {
            val upperId get() = id.uppercase()
        }

        val model = connection.transaction {
            execute("CREATE TABLE test(id TEXT PRIMARY KEY);")
            prepare("INSERT INTO test(id) VALUES (?)", "test")

            query("SELECT * FROM test")
                .single<TestModel>()
        }

        assertEquals("TEST", model.upperId)
    }

    @Test
    fun `direct model reference`() = runTest {
        @Model
        data class TestModel(
            val id: String,
            val hello: String
        )

        val model = TestModel("a", "b")
        connection.transaction {
            prepare("CREATE TABLE test(id TEXT PRIMARY KEY, hello TEXT);")
            prepare("INSERT INTO test(id, hello) VALUES (:id, :hello)", model)
        }

        val result = connection.transaction {
            prepare("SELECT * FROM test WHERE id = :id", model)
                .single<TestModel>()
        }

        assertEquals("a", result.id)
        assertEquals("b", result.hello)
    }

    @Test
    fun `test nullable inserts`() = runTest {
        @Model
        data class TestModel(
            val id: Int,
            val name: String?,
            val age: Int?
        )

        val model = TestModel(1, null, null)
        connection.transaction {
            prepare("CREATE TABLE test(id INTEGER PRIMARY KEY, name TEXT, age INTEGER);")
            prepare("INSERT INTO test(id, name, age) VALUES (:id, :name, :age)", model)
        }

        val result = connection.transaction {
            prepare("SELECT * FROM test WHERE id = :id", model)
                .single<TestModel>()
        }

        assertEquals(1, result.id)
        assertEquals(null, result.name)
        assertEquals(null, result.age)
    }

}