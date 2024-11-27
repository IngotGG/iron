package gg.ingot.iron.test.suites.transformations

import gg.ingot.iron.test.IronTest
import gg.ingot.iron.test.models.UUIDHolder
import io.kotest.core.annotation.AutoScan
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

@AutoScan
class AdapterTest: DescribeSpec({
    describe("Adapter Test") {
        val iron = IronTest.sqlite()

        beforeAny {
            iron.prepare("DROP TABLE IF EXISTS test")
        }

        it("adapt into sql properly") {
            iron.prepare("""
                CREATE TABLE test(
                    id STRING PRIMARY KEY,
                    value REAL NOT NULL
                )
            """.trimIndent())

            val uuidHolder = UUIDHolder(value = 10.0)
            iron.prepare("INSERT INTO test(id, value) VALUES(:id, :value)", uuidHolder)

            val data = iron.prepare("SELECT * FROM test LIMIT 1").single<UUIDHolder>()
            data.id shouldBe uuidHolder.id
            data.value shouldBe uuidHolder.value
        }
    }
})