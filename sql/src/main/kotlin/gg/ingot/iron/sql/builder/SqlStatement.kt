package gg.ingot.iron.sql.builder

import org.intellij.lang.annotations.Language

/**
 * A single statement to be executed.
 * @author santio
 * @since 2.0
 */
data class SqlStatement internal constructor(
    @Language("SQL")
    val sql: String,
    val values: List<Any?>,
)