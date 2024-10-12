package gg.ingot.iron.test

import gg.ingot.iron.Iron
import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.strategies.EnumTransformation

//import gg.ingot.iron.generated.Tables

enum class TestEnum {
    A, B, C
}

@Model(table = "users")
data class User(
    val name: String?,
    val age: Int = 20,
    val active: Boolean = true,
    @Column(enum = EnumTransformation.Ordinal::class)
    val test: TestEnum = TestEnum.A
)

fun main() {
    val iron = Iron("jdbc:sqlite::memory:").connect()
    val db = iron.blocking()
    db.prepare("CREATE TABLE IF NOT EXISTS users (name TEXT PRIMARY KEY, age INTEGER, active BOOLEAN, test INT)")
    db.prepare("INSERT INTO users (name, age, active, test) VALUES (?, ?, ?, ?)", "John Doe", 30, true, 0)

    // Debug to make sure the row is there
    iron.useBlocking {
        val result = it.prepareStatement("SELECT * FROM users").executeQuery()

        result.next()
        println("name: ${result.getString(1)}") // works
        it.close()
    }

    val names = db.prepare("SELECT name FROM users").all<String>()
    println(names)

    val user = db.prepare("SELECT * FROM users").all<User>()
    println(user)
}