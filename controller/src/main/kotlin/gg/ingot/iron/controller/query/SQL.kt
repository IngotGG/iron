@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package gg.ingot.iron.controller.query

import gg.ingot.iron.Iron
import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.expressions.filter.*
import gg.ingot.iron.sql.types.ExpColumn
import gg.ingot.iron.sql.types.column
import kotlin.internal.OnlyInputTypes
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField

@Suppress("unused")
class SQL<@OnlyInputTypes C: Any?>(
    internal val iron: Iron,
    private val table: SqlTable
) {
    private fun columnName(property: KProperty<*>): ExpColumn {
        return table.columns.find { it.field == property.javaField?.name }?.name
            ?. let { column(it) }
            ?: error("References a field that doesn't exist in the table '${table.name}', field: ${property.name}")
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.eq(value: T): SqlPredicate {
        return SqlPredicate(columnName(this) eq value)
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.neq(value: T): SqlPredicate {
        return SqlPredicate(columnName(this) neq value)
    }

    infix fun SqlPredicate.and(predicate: SqlPredicate): SqlPredicate {
        return SqlPredicate(this.condition and predicate.condition)
    }

    infix fun SqlPredicate.or(predicate: SqlPredicate): SqlPredicate {
        return SqlPredicate(this.condition or predicate.condition)
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.lt(value: Number): SqlPredicate {
        return SqlPredicate(columnName(this) lt value)
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.lte(value: Number): SqlPredicate {
        return SqlPredicate(columnName(this) lte value)
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.gt(value: Number): SqlPredicate {
        return SqlPredicate(columnName(this) gt value)
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.gte(value: Number): SqlPredicate {
        return SqlPredicate(columnName(this) gte value)
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.like(value: String): SqlPredicate {
        return SqlPredicate(columnName(this) like value)
    }

    @Suppress("SpellCheckingInspection")
    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.ilike(value: String): SqlPredicate {
        return SqlPredicate(columnName(this) ilike value)
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.inList(values: List<T>): SqlPredicate {
        return SqlPredicate(columnName(this).inList(values))
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.notInList(values: List<T>): SqlPredicate {
        return SqlPredicate(columnName(this).notInList(values))
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.between(range: Pair<Number, Number>): SqlPredicate {
        return SqlPredicate((columnName(this) gte range.first) and (columnName(this) lte range.second))
    }

}
