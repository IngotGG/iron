package gg.ingot.iron.sql.scopes.insert

import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.types.Expression

/**
 * The scope for the `INSERT` clause.
 * @author santio
 * @since 2.0
 */
interface InsertScope: Scope {

    infix fun into(table: String): IntoInsertScope
    infix fun into(table: SqlTable): IntoInsertScope
    fun into(table: String, alias: String): IntoInsertScope

    fun orIgnore(): ConditionedInsertScope
    fun orReplace(): ConditionedInsertScope
    fun orReplace(vararg primaryKey: String): ConditionedInsertScope
    fun orReplace(vararg primaryKey: Expression): ConditionedInsertScope
    fun orRollback(): ConditionedInsertScope
    fun orAbort(): ConditionedInsertScope
    fun orFail(): ConditionedInsertScope

}