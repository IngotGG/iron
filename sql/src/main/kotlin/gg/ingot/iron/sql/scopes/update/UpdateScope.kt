package gg.ingot.iron.sql.scopes.update

import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.types.Expression

/**
 * The scope for the `UPDATE` clause.
 * @author santio
 * @since 2.0
 */
interface UpdateScope: Scope {

    fun orIgnore(): ConditionedUpdateScope
    fun orReplace(): ConditionedUpdateScope
    fun orRollback(): ConditionedUpdateScope
    fun orAbort(): ConditionedUpdateScope
    fun orFail(): ConditionedUpdateScope

    fun set(column: String, value: Any?): SetUpdateScope
    fun set(column: Expression, value: Any?): SetUpdateScope
    fun set(mapping: Map<String, Any?>): SetUpdateScope

}