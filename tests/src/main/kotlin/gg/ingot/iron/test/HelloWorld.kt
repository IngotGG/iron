package gg.ingot.iron.test

import gg.ingot.iron.annotations.Model
import gg.ingot.iron.generated.Tables

@Model
data class User(
    val id: Int,
    val name: String
)

fun main() {
    Tables
    println("Hello World!")
}