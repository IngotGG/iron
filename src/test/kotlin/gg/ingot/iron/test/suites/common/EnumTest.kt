package gg.ingot.iron.test.suites.common
import gg.ingot.iron.IronSettings
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.bindings.Bindings
import gg.ingot.iron.bindings.bind
import gg.ingot.iron.strategies.EnumTransformation
import gg.ingot.iron.test.IronTest
import io.kotest.core.annotation.AutoScan
import io.kotest.core.spec.style.DescribeSpec

private enum class Permission {
    A, B, C
}

@Model(table = "simple_users")
private data class User(
    val id: Int,
    val permission: Permission
): Bindings

@AutoScan
class EnumTest: DescribeSpec({
    describe("Enum Test") {

        it("test enum by transforming by name") {
            val iron = IronTest.sqlite()
            val user = User(1, Permission.B)

            iron.prepare("CREATE TABLE simple_users(id INTEGER PRIMARY KEY, permission TEXT);")
            iron.prepare("INSERT INTO simple_users(id, permission) VALUES (:id, :permission);", bind(user))

            val result = iron.prepare("SELECT * FROM simple_users WHERE id = ?", user.id).single<User>()
            assert(result.permission == Permission.B)
        }

        it("test enum by transforming by ordinal") {
            val iron = IronTest.sqlite(IronSettings(
                enumTransformation = EnumTransformation.Ordinal
            ))

            val user = User(1, Permission.B)

            iron.prepare("CREATE TABLE simple_users(id INTEGER PRIMARY KEY, permission TEXT);")
            iron.prepare("INSERT INTO simple_users(id, permission) VALUES (:id, :permission);", bind(user))

            val result = iron.prepare("SELECT * FROM simple_users WHERE id = ?", user.id).single<User>()
            assert(result.permission == Permission.B)
        }
    }
})