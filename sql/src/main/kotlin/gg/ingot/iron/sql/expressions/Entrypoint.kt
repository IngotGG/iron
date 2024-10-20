package gg.ingot.iron.sql.expressions

import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.expressions.query.SelectSql
import gg.ingot.iron.sql.types.Reference
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
    fun select(): SelectSql {
        return select(listOf("*"))
    }

    /**
     * Selects the expression from the database, useful for running
     * mathematical operations or similar.
     * @param expression The expression to select
     * @return A type-safe API for chaining operations
     */
    fun select(expression: String): Sql {
        return modify(this) {
            append("SELECT $expression")
        }
    }

    /**
     * Selects the specified columns from the database.
     * @param columns The columns to select
     * @return A type-safe API for chaining operations
     */
    fun select(columns: List<String>): SelectSql {
        return select(*columns.map { column(it) }.toTypedArray())
    }

    /**
     * Selects the specified columns from the database.
     * @param columns The columns to select
     * @return A type-safe API for chaining operations
     * @see column
     */
    fun select(vararg columns: Reference): SelectSql {
        return modify(SelectSql(this)) {
            append("SELECT ${columns.joinToString(", ") { it.asString(sql) }}")
        }
    }
}