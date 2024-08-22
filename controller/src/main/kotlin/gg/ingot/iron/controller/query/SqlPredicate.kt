package gg.ingot.iron.controller.query

import gg.ingot.iron.sql.params.SqlParamsBuilder
import gg.ingot.iron.sql.params.sqlParams

class SqlPredicate internal constructor(
    val queries: List<String>,
    val values: Map<String, Any?>
) {
    internal fun params(): SqlParamsBuilder {
        return sqlParams(values.mapKeys { it.key.removePrefix(":") })
    }

    override fun toString(): String {
        return queries.joinToString(" AND ")
    }

    companion object {
        fun where(query: String, vararg values: Pair<String, Any?>): SqlPredicate {
            return SqlPredicate(listOf(query), values.toMap())
        }
    }
}
