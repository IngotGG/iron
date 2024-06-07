import com.google.gson.Gson
import gg.ingot.iron.Iron
import gg.ingot.iron.IronSettings
import gg.ingot.iron.annotations.Column
import gg.ingot.iron.serialization.ColumnDeserializer
import gg.ingot.iron.serialization.EnumColumnDeserializer
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
        data class ExampleResponse(
            @Column(json = true)
            val test: EmbeddedJson
        )

        val res = ironSerializationInstance.transaction {
            execute("CREATE TABLE example(id INTEGER PRIMARY KEY, test JSONB)")
            execute("INSERT INTO example(test) VALUES ('{\"test\": \"hello\"}')")
            query<ExampleResponse>("SELECT * FROM example LIMIT 1;")
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
            query<ExampleResponse>("SELECT * FROM example LIMIT 1;")
                .singleNullable()
        }

        assertNotNull(res)
        assertEquals("hello", res.test.test)
    }

    data class CustomType(val str: String)
    object CustomTypeDeserializer : ColumnDeserializer<String, CustomType> {
        override fun deserialize(value: String): CustomType = CustomType(value)
    }

    @Test
    fun `custom deserializer`() = runTest {
        val connection = Iron("jdbc:sqlite::memory:").connect()

        data class Response(
            @Column(deserializer = CustomTypeDeserializer::class)
            val example: CustomType
        )

        val res = connection.transaction {
            execute("CREATE TABLE example(id INTEGER PRIMARY KEY, example TEXT)")
            execute("INSERT INTO example(example) VALUES ('hello')")
            query<Response>("SELECT * FROM example LIMIT 1;")
                .singleNullable()
        }

        assertNotNull(res)
        assertEquals("hello", res.example.str)
    }

    private enum class TestEnum { EXAMPLE, EXAMPLE_2; }

    @Test
    fun `enum deserializer`() = runTest {
        data class Response(val example: TestEnum)
        val connection = Iron("jdbc:sqlite::memory:").connect()

        val res = connection.transaction {
            execute("CREATE TABLE example(id INTEGER PRIMARY KEY, example TEXT)")
            execute("INSERT INTO example(example) VALUES ('EXAMPLE')")
            query<Response>("SELECT * FROM example LIMIT 1;")
                .singleNullable()
        }

        assertNotNull(res)
        assertEquals(res.example, TestEnum.EXAMPLE)
    }

    @Test
    fun `enum deserializer fail`() = runTest {
        data class Response(val example: TestEnum)
        val connection = Iron("jdbc:sqlite::memory:").connect()

        try {
            connection.transaction {
                execute("CREATE TABLE example(id INTEGER PRIMARY KEY, example TEXT)")
                execute("INSERT INTO example(example) VALUES ('INVALID')")
                query<Response>("SELECT * FROM example LIMIT 1;")
                    .singleNullable()
            }
        } catch(ex: Exception) {
            assert(ex is IllegalArgumentException)
        }
    }
}