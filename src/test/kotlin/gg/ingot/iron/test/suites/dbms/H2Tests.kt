package gg.ingot.iron.test.suites.dbms

import gg.ingot.iron.test.IronTest
import gg.ingot.iron.test.models.User
import io.kotest.core.annotation.AutoScan
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

@AutoScan
class H2Tests: DescribeSpec({
    describe("H2") {
        val iron = IronTest.sqlite(IronTest.json())

        beforeAny {
            iron.prepare("DROP TABLE IF EXISTS test")
        }

        it("connect to database") {
            val result = iron.prepare("SELECT 1")
            result.single<Int>() shouldBe 1
        }

        it("work with arrays") {
            iron.prepare("CREATE TABLE test(id INTEGER PRIMARY KEY, data TEXT ARRAY)")
            iron.prepare("INSERT INTO test(id, data) VALUES (?, ?)", 1,  arrayOf("a", "b", "c"))

            val result = iron.prepare("SELECT data FROM test").single<Array<String>>()
            result shouldBe arrayOf("a", "b", "c")
        }

        it("work with collections") {
            iron.prepare("CREATE TABLE test(id INTEGER PRIMARY KEY, data TEXT ARRAY)")
            iron.prepare("INSERT INTO test(id, data) VALUES (?, ?)", 1,  listOf("a", "b", "c"))

            val result = iron.prepare("SELECT data FROM test").single<List<String>>()
            result shouldBe listOf("a", "b", "c")
        }

        it("work with model insertion") {
            val user = User(1, "John Doe", 30)
            iron.prepare(User.tableDefinition)
            iron.prepare("INSERT INTO users(id, name, age) VALUES (:id, :name, :age)", user)

            val result = iron.prepare("SELECT data FROM users").single<User>()
            result shouldBe user
        }

        it("work with integers") {
            iron.prepare("CREATE TABLE test(id INTEGER PRIMARY KEY, data INTEGER)")
            iron.prepare("INSERT INTO test(id, data) VALUES (?, ?)", 1, 1)

            val result = iron.prepare("SELECT data FROM test").single<Int>()
            result shouldBe 1
        }

        it("work with floats") {
            iron.prepare("CREATE TABLE test(id INTEGER PRIMARY KEY, data FLOAT)")
            iron.prepare("INSERT INTO test(id, data) VALUES (?, ?)", 1, 1.0f)

            val result = iron.prepare("SELECT data FROM test").single<Float>()
            result shouldBe 1.0f
        }

        it("work with doubles") {
            iron.prepare("CREATE TABLE test(id INTEGER PRIMARY KEY, data DOUBLE)")
            iron.prepare("INSERT INTO test(id, data) VALUES (?, ?)", 1, 1.0)

            val result = iron.prepare("SELECT data FROM test").single<Double>()
            result shouldBe 1.0
        }

        it("work with booleans") {
            iron.prepare("CREATE TABLE test(id INTEGER PRIMARY KEY, data BOOLEAN)")
            iron.prepare("INSERT INTO test(id, data) VALUES (?, ?)", 1, true)

            val result = iron.prepare("SELECT data FROM test").single<Boolean>()
            result shouldBe true
        }

        it("work with blobs") {
            iron.prepare("CREATE TABLE test(id INTEGER PRIMARY KEY, data BLOB)")
            iron.prepare("INSERT INTO test(id, data) VALUES (?, ?)", 1, byteArrayOf(1, 2, 3))

            val result = iron.prepare("SELECT data FROM test").single<ByteArray>()
            result shouldBe byteArrayOf(1, 2, 3)
        }

        it("work with json") {
            data class TestModel(val a: Int, val b: Int)
            val json = TestModel(1, 2)

            iron.prepare("CREATE TABLE test_json(id INTEGER PRIMARY KEY, data JSON)")
            iron.prepare("CREATE TABLE test_jsonb(id INTEGER PRIMARY KEY, data JSONB)")

            iron.prepare("INSERT INTO test_json(id, data) VALUES (?, ?)", 1, json)
            val result = iron.prepare("SELECT data FROM test_json").single<TestModel>()
            result shouldBe json
            
            iron.prepare("INSERT INTO test_jsonb(id, data) VALUES (?, ?)", 1, json)
            val result2 = iron.prepare("SELECT data FROM test_jsonb").single<TestModel>()
            result2 shouldBe json
        }
    }
})