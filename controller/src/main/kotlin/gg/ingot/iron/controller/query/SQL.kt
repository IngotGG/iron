@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package gg.ingot.iron.controller.query

import gg.ingot.iron.Iron
import gg.ingot.iron.models.SqlTable
import org.jooq.Operator
import org.jooq.impl.DSL
import kotlin.internal.OnlyInputTypes
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField

@Suppress("unused")
class SQL<@OnlyInputTypes C: Any?>(
    internal val iron: Iron,
    private val table: SqlTable
) {
    private fun columnName(property: KProperty<*>): String {
        return table.columns.find { it.field == property.javaField?.name }?.name
            ?: error("References a field that doesn't exist in the table '${table.name}', field: ${property.name}")
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.eq(value: T): SqlPredicate {
        return SqlPredicate(DSL.field(columnName(this)).eq(value))
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.neq(value: T): SqlPredicate {
        return SqlPredicate(DSL.field(columnName(this)).ne(value))
    }

    infix fun SqlPredicate.and(predicate: SqlPredicate): SqlPredicate {
        return SqlPredicate(DSL.condition(Operator.AND, this.condition, predicate.condition))
    }

    infix fun SqlPredicate.or(predicate: SqlPredicate): SqlPredicate {
        return SqlPredicate(DSL.condition(Operator.OR, this.condition, predicate.condition))
    }

    infix fun SqlPredicate.xor(predicate: SqlPredicate): SqlPredicate {
        return SqlPredicate(DSL.condition(Operator.XOR, this.condition, predicate.condition))
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.lt(value: T): SqlPredicate {
        return SqlPredicate(DSL.field(columnName(this)).lt(value))
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.lte(value: T): SqlPredicate {
        return SqlPredicate(DSL.field(columnName(this)).le(value))
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.gt(value: T): SqlPredicate {
        return SqlPredicate(DSL.field(columnName(this)).gt(value))
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.gte(value: T): SqlPredicate {
        return SqlPredicate(DSL.field(columnName(this)).ge(value))
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.like(value: String): SqlPredicate {
        return SqlPredicate(DSL.field(columnName(this)).like(value))
    }

    @Suppress("SpellCheckingInspection")
    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.ilike(value: String): SqlPredicate {
        return SqlPredicate(DSL.field(columnName(this)).likeIgnoreCase(value))
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.inList(values: List<T>): SqlPredicate {
        return SqlPredicate(DSL.field(columnName(this)).`in`(values))
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.notInList(values: List<T>): SqlPredicate {
        return SqlPredicate(DSL.field(columnName(this)).notIn(values))
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.between(range: Pair<T, T>): SqlPredicate {
        return SqlPredicate(DSL.field(columnName(this)).between(range.first, range.second))
    }

    operator fun SqlPredicate.not(): SqlPredicate {
        return SqlPredicate(DSL.not(this.condition))
    }

}
