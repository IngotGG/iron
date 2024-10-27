package gg.ingot.iron.sql.expressions.queries

import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.expressions.filter.Filter
import gg.ingot.iron.sql.scopes.update.ConditionedUpdateScope
import gg.ingot.iron.sql.scopes.update.SetUpdateScope
import gg.ingot.iron.sql.scopes.update.UpdateScope
import gg.ingot.iron.sql.scopes.update.WhereUpdateScope
import gg.ingot.iron.sql.types.ExpValue
import gg.ingot.iron.sql.types.Expression
import gg.ingot.iron.sql.types.column
import java.util.function.Supplier

internal class UpdateQuery(private val sql: Sql): Sql(sql.driver, sql.builder),
    UpdateScope, ConditionedUpdateScope, SetUpdateScope, WhereUpdateScope {
    override fun orIgnore(): ConditionedUpdateScope {
        return modify(this) {
            append("OR IGNORE")
        }
    }

    override fun orReplace(): ConditionedUpdateScope {
        return modify(this) {
            append("OR REPLACE")
        }
    }

    override fun orRollback(): ConditionedUpdateScope {
        return modify(this) {
            append("OR ROLLBACK")
        }
    }

    override fun orAbort(): ConditionedUpdateScope {
        return modify(this) {
            append("OR ABORT")
        }
    }

    override fun orFail(): ConditionedUpdateScope {
        return modify(this) {
            append("OR FAIL")
        }
    }

    override fun set(column: String, value: Any?): SetUpdateScope {
        return set(column(column), value)
    }

    override fun set(column: Expression, value: Any?): SetUpdateScope {
        return modify(this) {
            if (!contains("SET")) append("SET")
            else replace(-1, get(-1) + ",")

            append(column.asString(sql), "=", ExpValue.of(value).asString(sql))
        }
    }

    override fun set(mapping: Map<String, Any?>): SetUpdateScope {
        for ((column, value) in mapping) {
            set(column, value)
        }

        return this
    }

    override fun where(expression: String): WhereUpdateScope {
        return modify(this) {
            append("WHERE", expression)
        }
    }

    override fun where(filter: Supplier<Filter>): WhereUpdateScope {
        return where(filter.get())
    }

    override fun where(filter: Filter?): WhereUpdateScope {
        if (filter == null) return this
        return modify(this) {
            append("WHERE", filter.asString(sql))
        }
    }


}