package gg.ingot.iron.test.suites.controllers

import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.controller.controller.controller
import gg.ingot.iron.test.IronTest
import gg.ingot.iron.test.models.User
import io.kotest.core.spec.AutoScan
import io.kotest.core.spec.style.DescribeSpec

@AutoScan
class ControllerTest: DescribeSpec({
    describe("Controller Test") {
        val iron = IronTest.sqlite()

        beforeEach {
            iron.prepare("DROP TABLE IF EXISTS users")
            iron.prepare(User.tableDefinition)
        }

        it("insert & query") {
            val controller = iron.controller<User>()

            for (i in 0 until 10) {
                val user = User(i, "User $i", i + 18, "user${i + 1}@example.com")
                controller.insert(user)
            }

            val users = controller.all()
            assert(users.size == 10)
            assert(controller.count() == 10)

            controller.drop()
            try {
                controller.all()
                assert(false)
            } catch (e: Exception) {
                // Expected
                assert(true)
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

            assert(user?.name == "User 7")
            assert(user?.age == 25)
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

            assert(controller.count() == 8)

            controller.clear()

            assert(controller.count() == 0)
        }

        it("update") {
            val controller = iron.controller<User>()
            var user = User(1, "User 1", 18)

            controller.insert(user)
            user.age = 25

            controller.update(user)
            user = controller.first()!!

            assert(user.age == 25)
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

            assert(users.size == 4)
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
            assert(controller.count() == 10)
            assert(users.size == 10)
        }

        it("upsert with fetch") {
            val controller = iron.controller<User>()
            var user = User(1, "User 1", 18)

            user = controller.upsert(user, true)
            assert(controller.count() == 1)

            user.age = 25
            user = controller.upsert(user, true)
            assert(controller.count() == 1)

            assert(user.age == 25)
        }

        it("fetch with new id") {
            val controller = iron.controller<User>()
            val users = mutableListOf<User>()

            for (i in 0 until 10) {
                val user = User(null, "User $i", i + 18, "")
                users.add(controller.insert(user, fetch = true))
            }

            assert(users.size == 10)
            assert(controller.count() == 10)

            for (i in 0 until 10) {
                assert(users[i].id == i + 1)
            }
        }

        it("insert many then fetch") {
            val controller = iron.controller<User>()
            var users = (0 until 10).map { User(it, "User $it", it + 18, "") }

            users = controller.insertMany(users, true)
            assert(controller.count() == 10)
            assert(users.size == 10)

            for (i in 0 until 10) {
                assert(users[i].age == i + 18)
            }
        }

        it("insert with reserved keyword") {
            iron.prepare("CREATE TABLE tables (id TEXT PRIMARY KEY, 'default' INTEGER)")
            val controller = iron.controller<Table>()
            controller.insert(Table())

            val table = controller.first()
            assert(table?.id == "name")
            assert(table?.default == true)
        }

        it("upserting") {
            val controller = iron.controller<User>()
            val user = User(1, "User 1", 18)

            controller.upsert(user)
            assert(controller.count() == 1)

            user.age = 25
            controller.upsert(user)
            assert(controller.count() == 1)

            val updatedUser = controller.first()!!
            assert(updatedUser.age == 25)
        }
    }
})

@Model(table = "tables")
class Table {
    @Column(primaryKey = true)
    val id: String = "name"
    val default: Boolean = true
}