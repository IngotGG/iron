package gg.ingot.iron.test.suites.transformations

import gg.ingot.iron.strategies.NamingStrategy
import gg.ingot.iron.test.IronTest
import gg.ingot.iron.test.models.User
import io.kotest.core.annotation.AutoScan
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

@AutoScan
class TransformerTests: DescribeSpec({
    describe("Model Reader") {

        it("parse a model properly") {
            val iron = IronTest.sqlite()
            val reader = iron.modelReader

            val user = reader.read(User::class.java)
            val expected = listOf("id", "name", "age", "email", "createdAt", "updatedAt", "metadata")

            user.fields.map { it.name } shouldBe expected
        }

    }

    describe("Inflector") {
        it("properly handle naming strategies") {
            val expected = mapOf(
                NamingStrategy.SNAKE_CASE to "hello_world_foo_bar",
                NamingStrategy.CAMEL_CASE to "helloWorldFooBar",
                NamingStrategy.KEBAB_CASE to "hello-world-foo-bar",
                NamingStrategy.UPPER_CAMEL_CASE to "HelloWorldFooBar",
                NamingStrategy.UPPER_SNAKE_CASE to "HELLO_WORLD_FOO_BAR",
                NamingStrategy.UPPER_KEBAB_CASE to "HELLO-WORLD-FOO-BAR",
            )

            expected.forEach { (strategy, expected) ->
                strategy.transform("helloWorldFooBar") shouldBe expected
            }
        }

        it("handle pluralization") {
            val iron = IronTest.sqlite()
            val expected = mapOf(
                "user" to "users",
                "BannedUser" to "banned_users",
                "PersonWithLongName" to "person_with_long_names",
                "Person" to "persons",
                "Hello_World" to "hello_worlds",
            )

            expected.forEach { (input, output) ->
                iron.inflector.tableName(input) shouldBe output
            }
        }
    }
})