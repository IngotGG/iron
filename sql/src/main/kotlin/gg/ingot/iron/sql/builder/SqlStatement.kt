package gg.ingot.iron.sql.builder

import org.intellij.lang.annotations.Language

/**
 * A single statement to be executed.
 * @author santio
 * @since 2.0
 */
data class SqlStatement internal constructor(
    @Language("SQL")
    var sql: String,
    var values: List<Any?>,
    internal val builder: SqlBuilder
) {
    fun modify(function: SqlBuilder.() -> Unit) {
        function(builder)
        sql = builder.toString()
        values = builder.values()
    }
}