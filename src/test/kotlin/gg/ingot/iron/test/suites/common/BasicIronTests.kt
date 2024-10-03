package gg.ingot.iron.test.suites.common

import gg.ingot.iron.annotations.Model
import gg.ingot.iron.test.IronTest
import io.kotest.core.annotation.AutoScan
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.sql.SQLException
import kotlin.test.assertEquals

@Model
private data class TestModel(val id: Int, val name: String)

@AutoScan
class BasicIronTests: DescribeSpec({
    describe("Basic Iron Tests") {
        it("connect to database") {
            val iron = IronTest.sqlite()
            iron.use {
                it.createStatement().execute("SELECT 1") shouldBe true
            }
        }

        it("be able to query") {
            val iron = IronTest.sqlite()
            iron.query("SELECT 1").get<Int>(1) shouldBe 1
        }

        it("be able to execute") {
            val iron = IronTest.sqlite()
            iron.execute("CREATE TABLE test(id INTEGER PRIMARY KEY);")
            iron.execute("INSERT INTO test(id) VALUES (1)")
            iron.prepare("SELECT * FROM test").all<Int>().size shouldBe 1
        }

        it("be able to run a transaction") {
            val iron = IronTest.sqlite()
            iron.transaction {
                execute("CREATE TABLE test(id INTEGER PRIMARY KEY);")
                execute("INSERT INTO test(id) VALUES (1)")
                prepare("SELECT id FROM test").all<Int>().size
            } shouldBe 1
        }

        it("handle transaction errors") {
            val iron = IronTest.sqlite()

            try {
                iron.transaction {
                    execute("CREATE TABLE test (id INTEGER PRIMARY KEY)")
                    execute("INSERT INTO test VALUES (1)")
                    throw IllegalStateException("This is a test")
                }
            } catch (e: IllegalStateException) {
                e.message shouldBe "This is a test"
            }

            try {
                iron.query("SELECT * FROM test")
            } catch (e: Exception) {
                assert(e is SQLException)
            }
        }
    }

    describe("Basic Mapping Tests") {
        it("map multiple rows") {
            val iron = IronTest.sqlite()
            val tests = iron.transaction {
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

            tests.size shouldBe 6
            for ((index, test) in tests.withIndex()) {
                test.id shouldBe index + 1
                test.name shouldBe "test${index + 1}"
            }
        }

        it("map single row") {
            val iron = IronTest.sqlite()
            iron.transaction {
                afterCommit {
                    assert(true)
                }

                afterRollback {
                    assert(false)
                }

                execute("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT)")
                execute("INSERT INTO test VALUES (1, 'test1')")
                execute("INSERT INTO test VALUES (2, 'test2')")
                execute("INSERT INTO test VALUES (3, 'test3')")
                execute("INSERT INTO test VALUES (4, 'test4')")
                execute("INSERT INTO test VALUES (5, 'test5')")
                execute("INSERT INTO test VALUES (6, 'test6')")
            }

            val result = iron.prepare("SELECT * FROM test").getNext<TestModel>()
            assertEquals(1, result?.id)
            assertEquals("test1", result?.name)
        }

        it("throw exception when calling single with multiple rows") {
            val iron = IronTest.sqlite()

            try {
                iron.transaction {
                    afterCommit {
                        assert(false)
                    }

                    afterRollback {
                        assert(true)
                    }

                    execute("CREATE TABLE test (id INTEGER PRIMARY KEY)")
                    execute("INSERT INTO test VALUES (1), (2);")
                    prepare("SELECT * FROM test;")
                        .single<TestModel>()
                }
            } catch(ex: IllegalStateException) {
                assert(true)
            }
        }

        it("handle json") {
            data class NameHolder(val name: String)

            val iron = IronTest.sqlite(IronTest.json())

            iron.prepare("CREATE TABLE json (id INTEGER PRIMARY KEY, data TEXT)")
            iron.prepare("INSERT INTO json VALUES (1, '{\"name\": \"test\"}')")

            val result = iron.prepare("SELECT data FROM json").single<NameHolder>(json = true)
            assert(result.name == "test")
        }
    }
})