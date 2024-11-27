package gg.ingot.iron.controller.query

import gg.ingot.iron.sql.expressions.filter.Filter

data class SqlPredicate internal constructor(
    val condition: Filter
)

typealias SqlFilter<T> = SQL<T>.() -> SqlPredicate