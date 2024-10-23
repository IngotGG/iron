package gg.ingot.iron.test.suites.dbms

import gg.ingot.iron.test.IronTest
import gg.ingot.iron.test.models.Data
import gg.ingot.iron.test.models.JsonHolder
import gg.ingot.iron.test.models.User
import io.kotest.core.annotation.AutoScan
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

@AutoScan
class PostgresTests: DescribeSpec({
    describe("Postgres") {
        val iron = IronTest.postgres()

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

            val result = iron.prepare("SELECT data FROM test").single<ArrayList<String>>()
            result shouldBe listOf("a", "b", "c")
        }

        it("work with model insertion") {
            val user = User(1, "John Doe", 30)
            iron.prepare(User.tableDefinition)
            iron.prepare(
                "INSERT INTO users(id, name, age, created_at, updated_at) VALUES (:id, :name, :age, :createdAt, :updatedAt)",
                user.bindings()
            )

            val result = iron.prepare("SELECT * FROM users").single<User>()
            result shouldBe user
        }

        it("work with integers") {
            iron.prepare("CREATE TABLE test_int(id INTEGER PRIMARY KEY, data INTEGER)")
            iron.prepare("INSERT INTO test_int(id, data) VALUES (?, ?)", 1, 1)

            val result = iron.prepare("SELECT data FROM test_int").single<Int>()
            result shouldBe 1
        }

        it("work with floats") {
            iron.prepare("CREATE TABLE test_float(id INTEGER PRIMARY KEY, data FLOAT)")
            iron.prepare("INSERT INTO test_float(id, data) VALUES (?, ?)", 1, 1.0f)

            val result = iron.prepare("SELECT data FROM test_float").single<Float>()
            result shouldBe 1.0f
        }

        it("work with doubles") {
            iron.prepare("CREATE TABLE test_double(id INTEGER PRIMARY KEY, data DECIMAL)")
            iron.prepare("INSERT INTO test_double(id, data) VALUES (?, ?)", 1, 1.0)

            val result = iron.prepare("SELECT data FROM test_double").single<Double>()
            result shouldBe 1.0
        }

        it("work with booleans") {
            iron.prepare("CREATE TABLE test_bool(id INTEGER PRIMARY KEY, data BOOLEAN)")
            iron.prepare("INSERT INTO test_bool(id, data) VALUES (?, ?)", 1, true)

            val result = iron.prepare("SELECT data FROM test_bool").single<Boolean>()
            result shouldBe true
        }

        it("work with blobs") {
            iron.prepare("CREATE TABLE test_blob(id INTEGER PRIMARY KEY, data BYTEA)")
            iron.prepare("INSERT INTO test_blob(id, data) VALUES (?, ?)", 1, byteArrayOf(1, 2, 3))

            val result = iron.prepare("SELECT data FROM test_blob").single<ByteArray>()
            result shouldBe byteArrayOf(1, 2, 3)
        }

        it("work with json") {
            val holder = JsonHolder()

            iron.prepare("CREATE TABLE test_json(id INTEGER PRIMARY KEY, data JSON)")
            iron.prepare("CREATE TABLE test_jsonb(id INTEGER PRIMARY KEY, data JSONB)")

            iron.prepare("INSERT INTO test_json(id, data) VALUES (:id, CAST(:json AS JSON))", holder.bindings())
            val result = iron.prepare("SELECT data FROM test_json").single<Data>(json = true)
            result shouldBe holder.json
            
            iron.prepare("INSERT INTO test_jsonb(id, data) VALUES (:id, CAST(:json AS JSONB))", holder.bindings())
            val result2 = iron.prepare("SELECT data FROM test_jsonb").single<Data>(json = true)
            result2 shouldBe holder.json
        }
    }
})