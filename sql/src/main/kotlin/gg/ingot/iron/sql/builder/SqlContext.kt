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
     * Get the columns that are being either selected or inserted into.
     * @return The columns in the query.
     */
    fun columns(): List<String> {
        if (builder.get(0) == "INSERT") {
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

}