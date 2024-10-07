package gg.ingot.iron.test.suites.transformations

import com.google.gson.Gson
import gg.ingot.iron.Iron
import gg.ingot.iron.IronSettings
import gg.ingot.iron.serialization.SerializationAdapter
import gg.ingot.iron.test.IronTest
import gg.ingot.iron.test.models.JsonHolder
import io.kotest.core.annotation.AutoScan
import io.kotest.core.spec.style.DescribeSpec
import kotlinx.serialization.json.Json

private suspend fun runDeserializationTest(iron: Iron) {
    iron.prepare(
        "INSERT INTO test(id, json) VALUES (:id, :json);",
        1, """{"name": "John Doe", "age": 30}"""
    )

    val result = iron.prepare("SELECT json FROM test LIMIT 1;")
        .single<Map<String, Any?>>()

    assert(result["json"] is Map<*, *>)
    assert((result["json"] as Map<*, *>)["name"] as String == "John Doe")
    assert((result["json"] as Map<*, *>)["age"] as Int == 30)
}

@AutoScan
class SerializationTests: DescribeSpec({
    describe("Gson Serialization") {
        val iron = IronTest.sqlite(
            IronSettings(
                serialization = SerializationAdapter.Gson(Gson())
            )
        )

        beforeAny {
            iron.prepare("DROP TABLE IF EXISTS test")
        }

        it("deserialize json as json") {
            iron.prepare("CREATE TABLE test(id INTEGER PRIMARY KEY, json JSON);")
            runDeserializationTest(iron)
        }

        it("deserialize json as jsonb") {
            iron.prepare("CREATE TABLE test(id INTEGER PRIMARY KEY, json JSONB);")
            runDeserializationTest(iron)
        }

        it("serialize json") {
            iron.prepare("CREATE TABLE test(id INTEGER PRIMARY KEY, json TEXT);")

            val holder = JsonHolder()
            iron.prepare("INSERT INTO test(id, json) VALUES (:id, :json);", holder)
            val result = iron.prepare("SELECT * FROM test LIMIT 1;")
                .single<JsonHolder>()

            assert(result.json.name == holder.json.name)
            assert(result.json.age == holder.json.age)
        }
    }

    describe("Kotlinx Serialization") {
        val iron = IronTest.sqlite(
            IronSettings(
                serialization = SerializationAdapter.Kotlinx(Json)
            )
        )

        beforeAny {
            iron.prepare("DROP TABLE IF EXISTS test")
        }

        it("deserialize json as json") {
            iron.prepare("CREATE TABLE test(id INTEGER PRIMARY KEY, json JSON);")
            runDeserializationTest(iron)
        }

        it("deserialize json as jsonb") {
            iron.prepare("CREATE TABLE test(id INTEGER PRIMARY KEY, json JSONB);")
            runDeserializationTest(iron)
        }

        it("serialize json") {
            iron.prepare("CREATE TABLE test(id INTEGER PRIMARY KEY, json TEXT);")

            val holder = JsonHolder()
            iron.prepare("INSERT INTO test(id, json) VALUES (:id, :json);", holder)
            val result = iron.prepare("SELECT * FROM test LIMIT 1;")
                .single<JsonHolder>()

            assert(result.json.name == holder.json.name)
            assert(result.json.age == holder.json.age)
        }
    }
})