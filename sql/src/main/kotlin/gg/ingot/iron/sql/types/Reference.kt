package gg.ingot.iron.sql.types

import gg.ingot.iron.sql.Sql

abstract class Reference(
    val functions: MutableList<(Reference.(Sql) -> String)> = mutableListOf(),
    var alias: String? = null,
) {
    abstract fun asString(sql: Sql): String

    infix fun alias(alias: String): Reference {
        this.alias = alias
        return this
    }
}