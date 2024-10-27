package gg.ingot.iron.sql.scopes.update

import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.types.Expression

/**
 * The scope for the `UPDATE OR ...` clause.
 * @author santio
 * @since 2.0
 */
interface ConditionedUpdateScope: Scope {

    fun set(column: String, value: Any?): SetUpdateScope
    fun set(column: Expression, value: Any?): SetUpdateScope
    fun set(mapping: Map<String, Any?>): SetUpdateScope

}
