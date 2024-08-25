@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package gg.ingot.iron.controller.query

import gg.ingot.iron.Iron
import gg.ingot.iron.controller.engine.DBMSEngine
import gg.ingot.iron.representation.EntityModel
import kotlin.internal.OnlyInputTypes
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField

@Suppress("unused")
class SQL<@OnlyInputTypes C: Any?>(
    internal val iron: Iron,
    private val model: EntityModel,
    private val engine: DBMSEngine<*>
) {
    private var counter = 0

    private fun nextVariable(): String {
        return ":param${counter++}"
    }

    private fun columnName(property: KProperty<*>): String {
        val field = model.fields.find { it.field == property.javaField }!!
        return engine.column(field.columnName)
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.eq(value: T): SqlPredicate {
        val variable = nextVariable()
        return SqlPredicate.where("${columnName(this)} = $variable", variable to value)
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.neq(value: T): SqlPredicate {
        val variable = nextVariable()
        return SqlPredicate.where("${columnName(this)} != $variable", variable to value)
    }

    infix fun SqlPredicate.and(predicate: SqlPredicate): SqlPredicate {
        return SqlPredicate(queries + predicate.queries, values + predicate.values)
    }

    infix fun SqlPredicate.or(predicate: SqlPredicate): SqlPredicate {
        return SqlPredicate(listOf("(${queries.joinToString(" AND ")}) OR (${predicate.queries.joinToString(" AND ")})"), values + predicate.values)
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.lt(value: T): SqlPredicate {
        val variable = nextVariable()
        return SqlPredicate.where("${columnName(this)} < $variable", variable to value)
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.lte(value: T): SqlPredicate {
        val variable = nextVariable()
        return SqlPredicate.where("${columnName(this)} <= $variable", variable to value)
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.gt(value: T): SqlPredicate {
        val variable = nextVariable()
        return SqlPredicate.where("${columnName(this)} > $variable", variable to value)
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.gte(value: T): SqlPredicate {
        val variable = nextVariable()
        return SqlPredicate.where("${columnName(this)} >= $variable", variable to value)
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.like(value: T): SqlPredicate {
        val variable = nextVariable()
        return SqlPredicate.where("${columnName(this)} LIKE $variable", variable to value)
    }

    @Suppress("SpellCheckingInspection")
    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.ilike(value: T): SqlPredicate {
        val variable = nextVariable()
        return SqlPredicate.where("${columnName(this)} ILIKE $variable", variable to value)
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.inList(values: List<T>): SqlPredicate {
        val variables = values.associateBy { nextVariable() }
        return SqlPredicate.where("${columnName(this)} IN (${variables.keys.joinToString(",")})", *variables.map { it.key to it.value }.toTypedArray())
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.notInList(values: List<T>): SqlPredicate {
        val variables = values.associateBy { nextVariable() }
        return SqlPredicate.where("${columnName(this)} NOT IN (${variables.keys.joinToString(",")})", *variables.map { it.key to it.value }.toTypedArray())
    }

    infix fun <@OnlyInputTypes T: Any?> KProperty1<C, T>.between(range: Pair<T, T>): SqlPredicate {
        val start = nextVariable()
        val end = nextVariable()
        return SqlPredicate.where("${columnName(this)} BETWEEN $start AND $end", start to range.first, end to range.second)
    }

    operator fun SqlPredicate.not(): SqlPredicate {
        return SqlPredicate(listOf("NOT (${queries.joinToString(" AND ")})"), values)
    }

}
