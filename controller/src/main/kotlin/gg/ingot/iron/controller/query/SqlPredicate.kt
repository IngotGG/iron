package gg.ingot.iron.controller.query

import gg.ingot.iron.bindings.SqlBindings
import gg.ingot.iron.bindings.bind

class SqlPredicate internal constructor(
    val queries: List<String>,
    val values: Map<String, Any?>
) {
    internal fun bindings(): SqlBindings {
        return bind(values.mapKeys { it.key.removePrefix(":") })
    }

    override fun toString(): String {
        return queries.joinToString(" AND ")
    }

    companion object {
        private val columnRegex = Regex("`([^`]*)`")

        fun where(query: String, vararg values: Pair<String, Any?>): SqlPredicate {
            return SqlPredicate(listOf(query), values.toMap())
        }
    }
}

typealias SqlFilter<T> = SQL<T>.() -> SqlPredicate