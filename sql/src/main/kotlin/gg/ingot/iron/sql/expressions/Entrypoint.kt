package gg.ingot.iron.sql.expressions

import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.expressions.query.SelectQuery
import gg.ingot.iron.sql.scopes.select.SelectScope
import gg.ingot.iron.sql.types.Expression
import gg.ingot.iron.sql.types.column

@Suppress("MemberVisibilityCanBePrivate")
open class Entrypoint(
    private val sql: Sql,
): Sql(sql.driver, sql.builder) {
    override fun toString(): String {
        return builder.toString().trim()
    }

    /**
     * Selects all columns from the database.
     * @return A type-safe API for chaining operations
     */
    fun select(): SelectScope {
        return select("*")
    }

    /**
     * Selects the specified columns from the database.
     * @param columns The columns to select
     * @return A type-safe API for chaining operations
     */
    fun select(vararg columns: String): SelectScope {
        return select(*columns.map { column(it) }.toTypedArray())
    }

    /**
     * Selects the specified columns from the database.
     * @param columns The columns to select
     * @return A type-safe API for chaining operations
     * @see column
     */
    fun select(vararg columns: Expression): SelectScope {
        return modify(SelectQuery(this)) {
            append("SELECT", columns.joinToString(", ") { it.asString(sql) })
        }
    }
}