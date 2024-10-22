package gg.ingot.iron.sql.types

import gg.ingot.iron.sql.Sql

abstract class Expression(
    val functions: MutableList<(Expression.(Sql) -> String)> = mutableListOf(),
    var alias: String? = null,
) {
    abstract fun asString(sql: Sql): String

    infix fun alias(alias: String): Expression {
        this.alias = alias
        return this
    }
}