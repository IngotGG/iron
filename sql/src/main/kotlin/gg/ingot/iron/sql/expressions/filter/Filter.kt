package gg.ingot.iron.sql.expressions.filter

import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.types.RefValue
import gg.ingot.iron.sql.types.Reference

class Filter(
    private val lhs: Reference,
    private val rhs: Reference,
    private val operator: String,
): Reference() {

    override fun asString(sql: Sql): String {
        return "(${lhs.asString(sql)} $operator ${rhs.asString(sql)})"
    }

    infix fun and(other: Filter): Filter {
        return Filter(this, other, "AND")
    }

    infix fun or(other: Filter): Filter {
        return Filter(this, other, "OR")
    }

}

infix fun Reference.gt(value: Any): Filter {
    return Filter(this, RefValue.of(value), ">")
}

infix fun Reference.lt(value: Any): Filter {
    return Filter(this, RefValue.of(value), "<")
}

infix fun Reference.eq(value: Any): Filter {
    return Filter(this, RefValue.of(value), "=")
}

infix fun Reference.neq(value: Any): Filter {
    return Filter(this, RefValue.of(value), "!=")
}

infix fun Reference.gte(value: Any): Filter {
    return Filter(this, RefValue.of(value), ">=")
}

infix fun Reference.lte(value: Any): Filter {
    return Filter(this, RefValue.of(value), "<=")
}

infix fun Reference.like(value: Any): Filter {
    return Filter(this, RefValue.of(value), "LIKE")
}

infix fun Reference.ilike(value: Any): Filter {
    return Filter(this, RefValue.of(value), "ILIKE")
}

infix fun Reference.inList(values: List<Any>): Filter {
    return Filter(this, RefValue.of(values), "IN")
}

infix fun Reference.notInList(values: List<Any>): Filter {
    return Filter(this, RefValue.of(values), "NOT IN")
}