package gg.ingot.iron.sql.types

import gg.ingot.iron.sql.Sql
import java.util.function.Function

/**
 * Represents a value that can be selected from the database.
 * Example usage:
 * ```
 * Sql(DBMS.MYSQL).select(coalesce(column("nerd"), "abc"))
 * ```
 * where "abc" is wrapped in SelectValue when compiling the query.
 * @param value The value to select
 * @author santio
 * @since 2.0
 */
@Suppress("MemberVisibilityCanBePrivate")
data class ExpValue(
    val value: String?,
    val func: ((Sql) -> String)? = null
): Expression() {
    override fun asString(sql: Sql): String {
        val value = value ?: return func?.invoke(sql) ?: "NULL"

        val compiled = functions.fold(value) { acc, function ->
            function(ExpValue(acc), sql)
        }

        return if (alias == null) compiled
        else "$compiled AS ${sql.driver.literal(alias!!)}"
    }

    companion object {
        fun of(value: Any?): Expression {
            return when (value) {
                is Expression -> return value
                is String -> {
                    of { it.driver.string(value) }
                }
                is List<*> -> {
                    ExpValue("(${value.joinToString(", ")})")
                }
                else -> ExpValue(value.toString())
            }
        }

        fun raw(value: String): Expression {
            return ExpValue(value)
        }

        fun of(func: Function<Sql, String>): Expression {
            return ExpValue(null) { func.apply(it) }
        }

        fun of(func: ((Sql) -> String)): Expression {
            return ExpValue(null, func)
        }

        fun placeholder(): Expression {
            return raw("?")
        }
    }
}