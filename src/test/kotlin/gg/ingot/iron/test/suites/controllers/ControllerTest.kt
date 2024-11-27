package gg.ingot.iron.test.suites.controllers

import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.controller.controller.controller
import gg.ingot.iron.test.IronTest
import gg.ingot.iron.test.models.User
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.AutoScan
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

@AutoScan
class ControllerTest: DescribeSpec({
    describe("Controller Test") {
        val iron = IronTest.sqlite()

        beforeEach {
            iron.prepare("DROP TABLE IF EXISTS users")
            iron.prepare("DROP TABLE IF EXISTS enums")

            iron.prepare(User.tableDefinition)
            iron.prepare("CREATE TABLE enums (id INTEGER PRIMARY KEY, enum TEXT)")
        }

        it("insert & query") {
            val controller = iron.controller<User>()

            for (i in 0 until 10) {
                val user = User(i, "User $i", i + 18, "user${i + 1}@example.com")
                controller.insert(user)
            }

            val users = controller.all()
            users.size shouldBe 10
            controller.count() shouldBe 10

            controller.drop()

            shouldThrow<Exception> {
                controller.all()
            }
        }

        it("filter") {
            val controller = iron.controller<User>()

            for (i in 0 until 10) {
                val user = User(i, "User $i", i + 18, "user${i + 1}@example.com")
                controller.insert(user)
            }

            val user = controller.first {
                (User::age eq 25) and (User::name eq "User 7")
            }

            user?.name shouldBe "User 7"
            user?.age shouldBe 25
        }

        it("delete") {
            val controller = iron.controller<User>()

            for (i in 0 until 10) {
                val user = User(i, "User $i", i + 18, "")
                controller.insert(user)
            }

            controller.delete {
                (User::age eq 18) or (User::age eq 19)
            }

            controller.count() shouldBe 8

            controller.clear()
            controller.count() shouldBe 0
        }

        it("update") {
            val controller = iron.controller<User>()
            var user = User(1, "User 1", 18)

            controller.insert(user)
            user.age = 25

            controller.update(user)
            user = controller.first()!!

            user.age shouldBe 25
        }

        it("retrieve all") {
            val controller = iron.controller<User>()

            for (i in 0 until 10) {
                val user = User(i, "User $i", i + 18, "")
                controller.insert(user)
            }

            val users = controller.all {
                (User::age gt 20) and (User::age lt 25)
            }

            users.size shouldBe 4
        }

//        it("interceptors") {
//            val controller = iron.controller<User>()
//            var user = User(1, "User 1", 18)
//
//            controller.interceptor {
//                it.apply { it.age += 10 }
//            }
//
//            user = controller.insert(user, true)
//            assert(user.age == 28)
//
//            user.age = 30
//            user = controller.update(user, true)
//
//            assert(user.age == 40)
//        }

        it("insert many") {
            val controller = iron.controller<User>()
            var users = (0 until 10).map { User(it, "User $it", it + 18, "") }

            users = controller.insertMany(users)
            users.size shouldBe 10
            controller.count() shouldBe 10
        }

        it("upsert with fetch") {
            val controller = iron.controller<User>()
            var user = User(1, "User 1", 18)

            user = controller.upsert(user, true)
            controller.count() shouldBe 1

            user.age = 25
            user = controller.upsert(user, true)
            controller.count() shouldBe 1

            user.age shouldBe 25
        }

        it("fetch with new id") {
            val controller = iron.controller<User>()
            val users = mutableListOf<User>()

            for (i in 0 until 10) {
                val user = User(null, "User $i", i + 18, "")
                users.add(controller.insert(user, fetch = true))
            }

            users.size shouldBe 10
            controller.count() shouldBe 10

            for (i in 0 until 10) {
                users[i].id shouldBe i + 1
            }
        }

        it("insert many then fetch") {
            val controller = iron.controller<User>()
            var users = (0 until 10).map { User(it, "User $it", it + 18, "") }

            users = controller.insertMany(users, true)
            controller.count() shouldBe 10
            users.size shouldBe 10

            for (i in 0 until 10) {
                users[i].age shouldBe i + 18
            }
        }

        it("insert with reserved keyword") {
            iron.prepare("CREATE TABLE tables (id TEXT PRIMARY KEY, 'default' INTEGER)")
            val controller = iron.controller<Table>()
            controller.insert(Table())

            val table = controller.first()
            table?.id shouldBe "name"
            table?.default shouldBe true
        }

        it("upserting") {
            val controller = iron.controller<User>()
            val user = User(1, "User 1", 18)

            controller.upsert(user)
            controller.count() shouldBe 1

            user.age = 25
            controller.upsert(user)
            controller.count() shouldBe 1

            val updatedUser = controller.first()!!
            updatedUser.age shouldBe 25
        }

        it("work with enum & enum serialization") {
            val controller = iron.controller<EnumHolder>()
            val enumHolder = EnumHolder(1, Enum.ONE)

            controller.insert(enumHolder)
            controller.count() shouldBe 1

            val enumHolder1 = controller.first()
            enumHolder1?.enum shouldBe Enum.ONE

            val enumHolder2 = controller.first {
                (EnumHolder::enum eq Enum.TWO)
            }

            enumHolder2?.enum shouldBe Enum.ONE
        }
    }
})

@Model(table = "tables")
class Table {
    @Column(primaryKey = true)
    val id: String = "name"
    val default: Boolean = true
}

@Model(table = "enums")
data class EnumHolder(
    @Column(primaryKey = true)
    val id: Int,
    val enum: Enum
)

enum class Enum {
    ONE, TWO, THREE
}