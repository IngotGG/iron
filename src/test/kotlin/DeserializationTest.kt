import com.google.gson.Gson
import gg.ingot.iron.Iron
import gg.ingot.iron.IronSettings
import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.representation.ExplodingModel
import gg.ingot.iron.serialization.ColumnDeserializer
import gg.ingot.iron.serialization.SerializationAdapter
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DeserializationTest {
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

    data class CustomType(val str: String)
    object CustomTypeDeserializer : ColumnDeserializer<String, CustomType> {
        override fun fromDatabaseValue(value: String): CustomType = CustomType(value)
    }

    @Test
    fun `custom deserializer`() = runTest {
        val connection = Iron("jdbc:sqlite::memory:").connect()

        @Model
        data class Response(
            @Column(deserializer = CustomTypeDeserializer::class)
            val example: CustomType
        )

        val res = connection.transaction {
            execute("CREATE TABLE example(id INTEGER PRIMARY KEY, example TEXT)")
            execute("INSERT INTO example(example) VALUES ('hello')")
            query("SELECT * FROM example LIMIT 1;")
                .singleNullable<Response>()
        }

        assertNotNull(res)
        assertEquals("hello", res.example.str)
    }

    private enum class TestEnum { EXAMPLE, EXAMPLE_2; }

    @Test
    fun `enum deserializer`() = runTest {
        @Model
        data class Response(val example: TestEnum)
        val connection = Iron("jdbc:sqlite::memory:").connect()

        val res = connection.transaction {
            execute("CREATE TABLE example(id INTEGER PRIMARY KEY, example TEXT)")
            execute("INSERT INTO example(example) VALUES ('EXAMPLE')")
            query("SELECT * FROM example LIMIT 1;")
                .singleNullable<Response>()
        }

        assertNotNull(res)
        assertEquals(res.example, TestEnum.EXAMPLE)
    }

    @Test
    fun `enum deserializer fail`() = runTest {
        @Model
        data class Response(val example: TestEnum)
        val connection = Iron("jdbc:sqlite::memory:").connect()

        try {
            connection.transaction {
                execute("CREATE TABLE example(id INTEGER PRIMARY KEY, example TEXT)")
                execute("INSERT INTO example(example) VALUES ('INVALID')")
                query("SELECT * FROM example LIMIT 1;")
                    .singleNullable<Response>()
            }
        } catch(ex: Exception) {
            assert(ex is IllegalArgumentException)
        }
    }

    @Test
    fun `enum deserializer single`() = runTest {
        val connection = Iron("jdbc:sqlite::memory:").connect()

        val enumValue = connection.transaction {
            execute("CREATE TABLE example(id INTEGER PRIMARY KEY, example TEXT)")
            execute("INSERT INTO example(example) VALUES ('EXAMPLE')")

            query("SELECT example FROM example LIMIT 1;")
                .single<TestEnum>()
        }

        assertEquals(enumValue, TestEnum.EXAMPLE)
    }

    @Test
    fun `serialize kotlinx`() = runTest {
        val connection = Iron("jdbc:sqlite::memory:", IronSettings(
            serialization = SerializationAdapter.Kotlinx(Json)
        )).connect()

        @Serializable
        data class JsonObj(val example: String)

        @Model
        data class FakeModel(
            val id: Int,
            @Column(json = true)
            val json: JsonObj
        ) : ExplodingModel

        connection.execute("CREATE TABLE test(id INTEGER PRIMARY KEY, json TEXT);")
        connection.prepare("INSERT INTO test(id, json) VALUES (?, ?);", FakeModel(
            1,
            JsonObj("hello")
        ))

        val res = connection.query("SELECT * FROM test;")
        assertNotNull(res)
        res.next()

        val mapped = connection.query("SELECT * FROM test LIMIT 1;")
            .single<FakeModel>()

        assertEquals("hello", mapped.json.example)
    }

    @Test
    fun `deserialize ints as bools`() = runTest {
        @Model
        data class TestMapping(val bool: Boolean)

        val connection = Iron("jdbc:sqlite::memory:")
            .connect()

        connection.execute("CREATE TABLE test(id INTEGER PRIMARY KEY, bool BOOLEAN NOT NULL)")
        connection.execute("INSERT INTO test(bool) VALUES (true)")
        connection.execute("INSERT INTO test(bool) VALUES (false)")

        val booleans = connection.query("SELECT bool FROM test")
            .all<Boolean>()

        val modeledBooleans = connection.query("SELECT bool FROM test")
            .all<TestMapping>()
            .map { it.bool }

        assertEquals(listOf(true, false), booleans)
        assertEquals(listOf(true, false), modeledBooleans)
    }
}