package gg.ingot.iron.sql.types

import gg.ingot.iron.sql.Sql

/**
 * Represents an expression that gets converted to a string after being told what driver
 * to use.
 * @author santio
 * @since 2.0
 */
abstract class Expression(
    val functions: MutableList<(Expression.(Sql) -> String)> = mutableListOf(),
    var alias: String? = null,
) {
    abstract fun asString(sql: Sql): String

    /**
     * Alias the expression with the given alias. (AS `alias`)
     * @param alias The alias to alias the expression with.
     * @return The expression for chaining.
     */
    infix fun alias(alias: String): Expression {
        this.alias = alias
        return this
    }
}