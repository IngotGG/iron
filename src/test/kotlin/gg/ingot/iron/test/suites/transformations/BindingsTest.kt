package gg.ingot.iron.test.suites.transformations

import gg.ingot.iron.annotations.Model
import gg.ingot.iron.bindings.Bindings
import gg.ingot.iron.bindings.bind
import gg.ingot.iron.test.IronTest
import io.kotest.core.annotation.AutoScan
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Model
private data class TransformModel(val a: Int, val b: Int): Bindings

@AutoScan
class BindingsTest: DescribeSpec({
    describe("Bindings") {

        it("parse bindings properly") {
            val bindings = bind {
                "hello" to "world"
                "abc" to 2
                "json" to buildJsonObject {
                    put("name", "John Doe")
                    put("age", 30)
                }
            }

            bindings.map shouldBe mapOf(
                "hello" to "world",
                "abc" to 2,
                "json" to buildJsonObject {
                    put("name", "John Doe")
                    put("age", 30)
                }
            )
        }

        it("concatenate bindings") {
            val bindings = bind {
                "hello" to "world"
            }

            val concat = bind {
                "abc" to 2
            }

            bindings.concat(concat).map shouldBe mapOf(
                "hello" to "world",
                "abc" to 2
            )
        }

        it("concatenate models") {

            val bindings = bind {
                "hello" to "world"
                with(TransformModel(1, 2))
            }

            bindings.map shouldBe mapOf(
                "hello" to "world"
                // Doesn't include the model because it wasn't parsed
            )
        }

        it("parse bindings properly") {
            val iron = IronTest.sqlite()
            val bindings = bind {
                "hello" to "world"
                "abc" to 2
                "json" to buildJsonObject {
                    put("name", "John Doe")
                    put("age", 30)
                }
                with(TransformModel(1, 2))
            }

            val parsed = bindings.parse(iron)
            parsed shouldBe mapOf(
                "hello" to "world",
                "abc" to 2,
                "json" to buildJsonObject {
                    put("name", "John Doe")
                    put("age", 30)
                },
                "a" to 1,
                "b" to 2
            )
        }

    }

    describe("Bindings in queries") {

        it("handle simple bindings") {
            val iron = IronTest.sqlite()
            val bindings = bind {
                "a" to 2
                "b" to 4
            }

            val result = iron.prepare("SELECT :a + :b", bindings)
                .single<Int>()

            result shouldBe 6
        }

        it("handle multiple bindings") {
            val iron = IronTest.sqlite()
            val result = iron.prepare("SELECT :a + :b", bind("a" to 2), bind("b" to 4))
                .single<Int>()

            result shouldBe 6
        }

        it("handle model bindings") {
            val iron = IronTest.sqlite()
            val model = TransformModel(2, 4)

            val result = iron.prepare("SELECT :a + :b", bind(model))
                .single<Int>()

            result shouldBe 6
        }
    }
})