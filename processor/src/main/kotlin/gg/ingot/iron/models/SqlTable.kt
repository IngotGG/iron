package gg.ingot.iron.models

/**
 * Represents a table in the database that has all possible information about it.
 * @author santio
 * @since 2.0
 */
data class SqlTable(
    val name: String,
    val clazz: String,
    val columns: List<SqlColumn>,
    val hash: String,
)