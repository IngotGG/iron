package gg.ingot.iron.test.suites.transformations

import com.google.gson.Gson
import gg.ingot.iron.Iron
import gg.ingot.iron.IronSettings
import gg.ingot.iron.serialization.SerializationAdapter
import gg.ingot.iron.test.IronTest
import gg.ingot.iron.test.models.Data
import gg.ingot.iron.test.models.JsonHolder
import io.kotest.core.annotation.AutoScan
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

private suspend fun runDeserializationTest(iron: Iron) {
    val data = JsonHolder()

    iron.prepare(
        "INSERT INTO test(id, json) VALUES (:id, :json)",
        data.bindings()
    )

    val result = iron.prepare("SELECT json FROM test LIMIT 1;")
        .single<Data>(json = true)

    assert(result.name == data.json.name)
    assert(result.age == data.json.age)
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
            iron.prepare("INSERT INTO test(id, json) VALUES (:id, :json);", holder.bindings())
            val result = iron.prepare("SELECT * FROM test LIMIT 1;")
                .single<JsonHolder>()

            result shouldBe holder
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
            iron.prepare("INSERT INTO test(id, json) VALUES (:id, :json);", holder.bindings())
            val result = iron.prepare("SELECT * FROM test LIMIT 1;")
                .single<JsonHolder>()

            result shouldBe holder
        }
    }
})