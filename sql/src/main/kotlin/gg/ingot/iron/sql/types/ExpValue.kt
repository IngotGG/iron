package gg.ingot.iron.sql.types

import gg.ingot.iron.sql.Sql

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
data class ExpValue(
    val value: String,
): Expression() {
    override fun asString(sql: Sql): String {
        val compiled = functions.fold(value) { acc, function ->
            function(ExpValue(acc), sql)
        }

        return if (alias == null) compiled
        else "$compiled AS ${sql.driver.literal(alias!!)}"
    }

    companion object {
        fun of(value: Any): Expression {
            return when (value) {
                is Expression -> return value
                is String -> {
                    ExpValue(
                        "\"${
                            value.replace("\\", "\\\\")
                                .replace("\"", "\\\"")
                                .replace("\n", "\\n")
                        }\""
                    )
                }
                is List<*> -> {
                    ExpValue("(${value.joinToString(", ")})")
                }
                else -> ExpValue(value.toString())
            }
        }
    }
}