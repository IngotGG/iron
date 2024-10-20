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
data class RefValue(
    val value: String,
): Reference() {
    override fun asString(sql: Sql): String {
        val compiled = functions.fold(value) { acc, function ->
            function(RefValue(acc), sql)
        }

        return if (alias == null) compiled
        else "$compiled AS ${sql.driver.literal(alias!!)}"
    }

    companion object {
        fun of(value: Any): Reference {
            return when (value) {
                is Reference -> return value
                is String -> {
                    RefValue(
                        "\"${
                            value.replace("\\", "\\\\")
                                .replace("\"", "\\\"")
                                .replace("\n", "\\n")
                        }\""
                    )
                }
                is List<*> -> {
                    RefValue("(${value.joinToString(", ")})")
                }
                else -> RefValue(value.toString())
            }
        }
    }
}