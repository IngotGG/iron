package gg.ingot.iron.sql.scopes.insert

import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.scopes.Scope

/**
 * The scope for the `INSERT OR` clause.
 * @author santio
 * @since 2.0
 */
interface ConditionedInsertScope: Scope {

    fun into(table: String): IntoInsertScope
    fun into(table: String, alias: String): IntoInsertScope
    fun into(table: SqlTable): IntoInsertScope

}