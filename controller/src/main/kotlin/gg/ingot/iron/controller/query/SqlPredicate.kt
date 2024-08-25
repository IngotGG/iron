package gg.ingot.iron.controller.query

import gg.ingot.iron.controller.engine.DBMSEngine
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

    fun toString(engine: DBMSEngine<*>): String {
        return queries.joinToString(" AND ") { query ->
            columnRegex.replace(query) { match ->
                engine.column(match.groupValues[1])
            }
        }
    }

    companion object {
        private val columnRegex = Regex("`([^`]*)`")

        fun where(query: String, vararg values: Pair<String, Any?>): SqlPredicate {
            return SqlPredicate(listOf(query), values.toMap())
        }
    }
}

typealias SqlFilter<T> = SQL<T>.() -> SqlPredicate