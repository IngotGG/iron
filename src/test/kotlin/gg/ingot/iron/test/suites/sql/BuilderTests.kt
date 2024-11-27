package gg.ingot.iron.test.suites.sql

import gg.ingot.iron.DBMS
import gg.ingot.iron.Iron
import gg.ingot.iron.sql.expressions.filter.eq
import gg.ingot.iron.sql.expressions.filter.inList
import gg.ingot.iron.sql.expressions.ordering.desc
import gg.ingot.iron.sql.types.*
import gg.ingot.iron.test.IronTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.AutoScan
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.matchers.shouldBe

@AutoScan
class BuilderTests: DescribeSpec({
    describe("H2 Builders") {
        val iron = IronTest.h2()

        beforeAny {
            iron.transaction {
                prepare("DROP TABLE IF EXISTS sql_users")
                prepare("DROP TABLE IF EXISTS sql_ids")

                prepare("CREATE TABLE IF NOT EXISTS sql_users (name TEXT PRIMARY KEY, age INTEGER, active BOOLEAN)")
                prepare("INSERT INTO sql_users (name, age, active) VALUES (?, ?, ?)", "John Doe", 30, true)

                prepare("CREATE TABLE IF NOT EXISTS sql_ids (id INTEGER PRIMARY KEY, name TEXT)")
                prepare("INSERT INTO sql_ids (id, name) VALUES (?, ?)", 1, "John Doe")
                prepare("INSERT INTO sql_ids (id, name) VALUES (?, ?)", 2, "Jane Doe")
                prepare("INSERT INTO sql_ids (id, name) VALUES (?, ?)", 3, "Bob Doe")
            }
        }

        runTests(iron)
    }

    describe("Sqlite Builders") {
        val iron = IronTest.sqlite()

        beforeAny {
            iron.transaction {
                prepare("DROP TABLE IF EXISTS sql_users")
                prepare("DROP TABLE IF EXISTS sql_ids")

                prepare("CREATE TABLE IF NOT EXISTS sql_users (name TEXT PRIMARY KEY, age INTEGER, active BOOLEAN)")
                prepare("INSERT INTO sql_users (name, age, active) VALUES (?, ?, ?)", "John Doe", 30, true)

                prepare("CREATE TABLE IF NOT EXISTS sql_ids (id INTEGER PRIMARY KEY, name TEXT)")
                prepare("INSERT INTO sql_ids (id, name) VALUES (?, ?)", 1, "John Doe")
                prepare("INSERT INTO sql_ids (id, name) VALUES (?, ?)", 2, "Jane Doe")
                prepare("INSERT INTO sql_ids (id, name) VALUES (?, ?)", 3, "Bob Doe")
            }
        }

        runTests(iron)
    }

    describe("Postgres Builders") {
        val iron = IronTest.postgres()

        beforeAny {
            iron.transaction {
                prepare("DROP TABLE IF EXISTS sql_users")
                prepare("DROP TABLE IF EXISTS sql_ids")

                prepare("CREATE TABLE IF NOT EXISTS sql_users (name TEXT PRIMARY KEY, age INTEGER, active BOOLEAN)")
                prepare("INSERT INTO sql_users (name, age, active) VALUES (?, ?, ?)", "John Doe", 30, true)

                prepare("CREATE TABLE IF NOT EXISTS sql_ids (id INTEGER PRIMARY KEY, name TEXT)")
                prepare("INSERT INTO sql_ids (id, name) VALUES (?, ?)", 1, "John Doe")
                prepare("INSERT INTO sql_ids (id, name) VALUES (?, ?)", 2, "Jane Doe")
                prepare("INSERT INTO sql_ids (id, name) VALUES (?, ?)", 3, "Bob Doe")
            }
        }

        runTests(iron)
    }

    describe("MySQL Builders") {
        val iron = IronTest.mysql()

        beforeAny {
            iron.transaction {
                prepare("DROP TABLE IF EXISTS sql_users")
                prepare("DROP TABLE IF EXISTS sql_ids")

                prepare("CREATE TABLE IF NOT EXISTS sql_users (name VARCHAR(255) PRIMARY KEY, age INTEGER, active BOOLEAN)")
                prepare("INSERT INTO sql_users (name, age, active) VALUES (?, ?, ?)", "John Doe", 30, true)

                prepare("CREATE TABLE IF NOT EXISTS sql_ids (id INTEGER PRIMARY KEY, name VARCHAR(255))")
                prepare("INSERT INTO sql_ids (id, name) VALUES (?, ?)", 1, "John Doe")
                prepare("INSERT INTO sql_ids (id, name) VALUES (?, ?)", 2, "Jane Doe")
                prepare("INSERT INTO sql_ids (id, name) VALUES (?, ?)", 3, "Bob Doe")
            }
        }

        runTests(iron)
    }
})

private suspend fun DescribeSpecContainerScope.runTests(iron: Iron) {
    it("query") {
        val result = iron.run {
            select("name") from "sql_users" where { column("active") eq true }
        }.single<String>()

        result shouldBe "John Doe"
    }

    it("query with subquery") {
        val result = iron.run {
            select("name") from "sql_users" where {
                column("name") inList { select("name") from "sql_ids" }
            }
        }.all<String>()

        result shouldBe listOf("John Doe")
    }

    it("query with limit") {
        val result = iron.run {
            select("name") from "sql_ids" orderBy desc("id") limit 1
        }.single<String>()

        result shouldBe "Bob Doe"
    }

    it("query with offset") {
        val result = iron.run {
            select("name") from "sql_ids" orderBy desc("id") limit 1 offset 1
        }.single<String>()

        result shouldBe "Jane Doe"
    }

    it("query with join") {
        val result = iron.run {
            select(count("*"))
                .from("sql_users")
                .alias("users")
                .join { select() from "sql_ids" }
                .alias("ids")
                .on { column("users", "name") eq column("ids", "name") }
        }.single<Int>()

        result shouldBe 1
    }

    it("query math") {
        iron.run {
            select(ceil(20.5))
        }.single<Double>() shouldBe 21.0

        iron.run {
            select(floor(20.5))
        }.single<Double>() shouldBe 20.0

        iron.run {
            select(round(20.5))
        }.single<Double>() shouldBe 21.0

        iron.run {
            select(abs(-20.5))
        }.single<Double>() shouldBe 20.5

        iron.run {
            select(coalesce(null, 20.5))
        }.single<Double>() shouldBe 20.5

        iron.run {
            select(coalesce(null, null, 20.5))
        }.single<Double>() shouldBe 20.5
    }

    it("insert") {
        iron.run {
            insert()
                .into("sql_users")
                .columns("name", "age", "active")
                .values("Jane Doe", 31, false)
        }

        val result = iron.run {
            select(count("*")) from "sql_users"
        }.single<Int>()

        result shouldBe 2
    }

    it("insert with returning") {
        @Suppress("SqlWithoutWhere")
        iron.prepare("DELETE FROM sql_users")

        val result = iron.run {
            insert()
                .into("sql_users")
                .columns("name", "age", "active")
                .values("Bob Doe", 32, true)
                .returning("age")
        }.single<Int>()

        result shouldBe 32
    }

    it("insert many with returning") {
        @Suppress("SqlWithoutWhere")
        iron.prepare("DELETE FROM sql_users")

        val result = iron.run {
            insert()
                .into("sql_users")
                .columns("name", "age", "active")
                .values("Bob Doe", 32, true)
                .values("Jane Doe", 31, false)
                .returning("age")
        }.all<Int>()

        result shouldBe listOf(32, 31)
    }

    it("insert without columns") {
        @Suppress("SqlWithoutWhere")
        iron.prepare("DELETE FROM sql_users")

        iron.run {
            insert()
                .into("sql_users")
                .values("Bob Doe", 32, true)
                .values("Jane Doe", 31, false)
        }

        val result = iron.run {
            select(count("*")) from "sql_users"
        }.single<Int>()

        result shouldBe 2
    }

    it("insert without columns failure") {
        @Suppress("SqlWithoutWhere")
        iron.prepare("DELETE FROM sql_users")

        // The databases that we test which support the RETURNING clause
        val supportsReturning = listOf(DBMS.SQLITE, DBMS.POSTGRESQL, DBMS.MARIADB)
            .contains(iron.settings.driver)

        if (supportsReturning) {
            val result = iron.run {
                insert()
                    .into("sql_users")
                    .values("Bob Doe", 32, true)
                    .values("Jane Doe", 31, false)
                    .returning("age")
            }.all<Int>()

            result shouldBe listOf(32, 31)
        } else {
            shouldThrow<Exception> {
                iron.run {
                    insert()
                        .into("sql_users")
                        .values("Bob Doe", 32, true)
                        .values("Jane Doe", 31, false)
                        .returning("age")
                }
            }
        }
    }

    it("upsert") {
        @Suppress("SqlWithoutWhere")
        iron.prepare("DELETE FROM sql_users")

        iron.run {
            insert()
                .orReplace("name")
                .into("sql_users")
                .columns("name", "age", "active")
                .values("Bob Doe", 32, true)
        }

        val result = iron.run {
            insert()
                .orReplace("name")
                .into("sql_users")
                .columns("name", "age", "active")
                .values("Bob Doe", 33, true)
                .returning("age")
        }.single<Int>()

        result shouldBe 33
    }

    it("update") {
        @Suppress("SqlWithoutWhere")
        iron.prepare("DELETE FROM sql_users")

        iron.run {
            insert()
                .into("sql_users")
                .columns("name", "age", "active")
                .values("Bob Doe", 32, false)
        }

        iron.run {
            update("sql_users")
                .set("active", true)
                .where { column("name") eq "Bob Doe" }
        }

        val result = iron.run {
            select("name") from "sql_users" where { column("active") eq true }
        }.single<String>()

        result shouldBe "Bob Doe"
    }

    it("rename table") {
        iron.run {
            alter().table("sql_users").rename("sql_users_renamed")
        }

        iron.run {
            alter().table("sql_users_renamed").rename("sql_users")
        }
    }

    // TODO: Unit test more altering

}