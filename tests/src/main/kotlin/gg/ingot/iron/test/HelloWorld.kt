package gg.ingot.iron.test

import gg.ingot.iron.annotations.Model
import gg.ingot.iron.generated.Tables.table

//import gg.ingot.iron.generated.Tables

@Model(table = "users")
data class User(
    val id: Int,
    val name: String?,
    val age: Int = 20
)

@Model(table = "my_users_a")
data class User2(
    val id: Int,
    val name: String?,
    val age: Int = 20
)

fun main() {
    val user2Table = User2::class.table
}