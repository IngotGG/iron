package gg.ingot.iron.sql.scopes

import gg.ingot.iron.sql.scopes.select.SelectScope

/**
 * A representation of a scope in the SQL query. This attempts to prevent the user from
 * creating invalid SQL queries.
 * @author santio
 * @since 2.0
 */
interface Scope {
    /**
     * @return The string representation of the generated SQL query
     */
    override fun toString(): String
}

fun main() {
    val select: SelectScope = null!!
}