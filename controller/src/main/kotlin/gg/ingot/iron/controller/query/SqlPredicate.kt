package gg.ingot.iron.controller.query

import gg.ingot.iron.controller.controller.TableController
import org.jooq.*
import org.jooq.impl.DSL

class SqlPredicate internal constructor(
    val condition: Condition
) {
    internal companion object {
        fun <T: Any> SelectJoinStep<out Record>.where(controller: TableController<T>, filter: SqlFilter<T>?): SelectConditionStep<out Record> {
            val sql = filter?.invoke(SQL(controller.iron, controller.table))?.condition
            return this.where(sql ?: DSL.trueCondition())
        }

        fun <T: Any> DeleteUsingStep<out Record>.where(controller: TableController<T>, filter: SqlFilter<T>?): DeleteConditionStep<out Record> {
            val sql = filter?.invoke(SQL(controller.iron, controller.table))?.condition
            return this.where(sql ?: DSL.trueCondition())
        }

        fun <T: Any> DSLContext.fetchCount(table: Table<Record>, controller: TableController<T>, filter: SqlFilter<T>?): Int {
            val sql = filter?.invoke(SQL(controller.iron, controller.table))?.condition
            return this.fetchCount(table, sql ?: DSL.trueCondition())
        }
    }
}

typealias SqlFilter<T> = SQL<T>.() -> SqlPredicate