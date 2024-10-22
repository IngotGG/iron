package gg.ingot.iron.sql.expressions.filter

import gg.ingot.iron.DBMS
import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.types.ExpValue
import gg.ingot.iron.sql.types.Expression

class Filter(
    private val lhs: Expression,
    private val rhs: Expression,
    private val operator: (driver: DBMS) -> String,
): Expression() {

    internal constructor(): this(ExpValue.of(true), ExpValue.of(true), { "AND" })

    override fun asString(sql: Sql): String {
        return "(${lhs.asString(sql)} ${operator.invoke(sql.driver)} ${rhs.asString(sql)})"
    }

    infix fun and(other: Filter): Filter {
        return Filter(this, other) { "AND" }
    }

    infix fun or(other: Filter): Filter {
        return Filter(this, other) { "OR" }
    }

}

infix fun Expression.gt(value: Any): Filter {
    return Filter(this, ExpValue.of(value)) { ">" }
}

infix fun Expression.lt(value: Any): Filter {
    return Filter(this, ExpValue.of(value)) { "<" }
}

infix fun Expression.eq(value: Any): Filter {
    return Filter(this, ExpValue.of(value)) { "=" }
}

infix fun Expression.neq(value: Any): Filter {
    return Filter(this, ExpValue.of(value)) { "!=" }
}

infix fun Expression.gte(value: Any): Filter {
    return Filter(this, ExpValue.of(value)) { ">=" }
}

infix fun Expression.lte(value: Any): Filter {
    return Filter(this, ExpValue.of(value)) { "<=" }
}

infix fun Expression.like(value: Any): Filter {
    return Filter(this, ExpValue.of(value)) { "LIKE" }
}

infix fun Expression.ilike(value: Any): Filter {
    return Filter(this, ExpValue.of(value)) { "ILIKE" }
}

infix fun Expression.inList(values: List<Any>): Filter {
    return Filter(this, ExpValue.of(values)) { "IN" }
}

infix fun Expression.notInList(values: List<Any>): Filter {
    return Filter(this, ExpValue.of(values)) { "NOT IN" }
}