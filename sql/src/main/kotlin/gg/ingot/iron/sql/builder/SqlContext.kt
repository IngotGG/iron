package gg.ingot.iron.sql.builder

/**
 * A utility class for attempting to understand context in a SQL query.
 * @author santio
 * @since 2.0
 */
class SqlContext internal constructor(
    private val builder: SqlBuilder
) {

    /**
     * Get the table name from the query.
     * @return The table name in the query.
     */
    fun table(): String? {
        val index = builder.lastIndexOf("FROM").takeIf { it != -1 }
            ?: builder.firstIndexOf("INTO").takeIf { it != -1 }
            ?: return null

        val name = builder.get(index + 1)
            ?: return null

        return builder.driver.real(name)
    }

    /**
     * Checks if the query is an insert or update query
     * @return Whether the query is an insert or update query
     */
    fun isUpdate(): Boolean {
        val updateKeywords = listOf("INSERT", "UPDATE", "MERGE", "REPLACE")
        return updateKeywords.contains(builder.get(0))
    }

    /**
     * Get the columns that are being either selected or inserted into.
     * @return The columns in the query.
     */
    fun columns(): List<String> {
        if (isUpdate()) {
            val tableIndex = builder.firstIndexOf("INTO") + 1
            val columns = builder.get(tableIndex + 1)
                ?.removeSurrounding("(", ")")
                ?.split(",")
                ?.map { builder.driver.real(it.trim()) }
                ?: emptyList()

            return columns
        } else if (builder.get(0) == "SELECT") {
            val columns = builder.get(1)
                ?.removeSurrounding("(", ")")
                ?.split(",")
                ?.map { builder.driver.real(it.trim()) }
                ?: emptyList()

            return columns
        } else return emptyList()
    }

    /**
     * Get the limit from the query.
     * @return The limit in the query, or -1 if no limit was specified.
     */
    fun limit(): Int {
        return builder.get(builder.lastIndexOf("LIMIT") + 1)
            ?.toIntOrNull()
            ?: -1
    }

    /**
     * Gets the changes or values being set in the query. (ex: SET column = value or VALUES (value, value))
     * @return The changes or values being set in the statement. If you are inserting or updating multiple rows
     * then this will return a list of maps, where the keys are the column names and the values are the values,
     * otherwise the list will only contain a single map.
     */
    fun changes(): List<Map<String, Any?>> {
        // Insert Query
        if (builder.get(0) == "INSERT") {
            val tableIndex = builder.firstIndexOf("INTO") + 1
            val columns = builder.get(tableIndex + 1)
                ?.removeSurrounding("(", ")")
                ?.split(",")
                ?.map { builder.driver.real(it.trim()) }
                ?: emptyList()

            val values = builder.values()

            return values.chunked(columns.size).map {
                columns.zip(it).toMap()
            }
        } else if (builder.contains("SET")) {
            // SET column = value, column = value
            val setIndex = builder.lastIndexOf("SET")
            val values = mutableMapOf<String, Any?>()

            var index = setIndex

            while (true) {
                if (builder.get(index + 2) != "=") break

                val column = builder.get(index + 1)
                    ?: error("Failed to get column name from SET statement")

                val value = builder.get(index + 3)
                    ?: error("Failed to get value from SET statement")

                values[column] = builder.driver.real(value)
                index += 4
            }

            return listOf(values)
        } else return emptyList()
    }

}